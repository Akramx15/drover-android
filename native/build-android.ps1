[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $SourceDir,

    [string] $NdkRoot,

    [string] $TargetDir,

    [long] $SourceDateEpoch = 1787541405,

    [switch] $SkipHostTests
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$UpstreamCommit = 'fc77ca3182b3a63b84266bb0a5d24c096e022765'
$RequiredNdkVersion = '26.1.10909125'
$RequiredRustVersion = '1.97.1'
$AndroidApi = 29
$Targets = @(
    [pscustomobject]@{
        RustTarget = 'aarch64-linux-android'
        ClangTarget = 'aarch64-linux-android'
        AndroidAbi = 'arm64-v8a'
    },
    [pscustomobject]@{
        RustTarget = 'armv7-linux-androideabi'
        ClangTarget = 'armv7a-linux-androideabi'
        AndroidAbi = 'armeabi-v7a'
    },
    [pscustomobject]@{
        RustTarget = 'x86_64-linux-android'
        ClangTarget = 'x86_64-linux-android'
        AndroidAbi = 'x86_64'
    }
)

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Executable,

        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]] $Arguments
    )

    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Executable $($Arguments -join ' ')"
    }
}

function Resolve-ExactToolchainCommand {
    param([string] $ToolName)

    $rustup = Get-Command rustup -ErrorAction SilentlyContinue
    if ($null -ne $rustup) {
        if ($ToolName -eq 'cargo') {
            # Consume rustup's success-stream output so this resolver returns
            # only the executable path. Fresh Linux runners print installation
            # progress there, which would otherwise turn $Cargo into an array.
            Invoke-Checked $rustup.Source toolchain install $RequiredRustVersion --profile minimal --component rustfmt --no-self-update | Out-Host
            foreach ($target in $Targets) {
                Invoke-Checked $rustup.Source target add --toolchain $RequiredRustVersion $target.RustTarget | Out-Host
            }
        }
        $resolved = (& $rustup.Source which --toolchain $RequiredRustVersion $ToolName).Trim()
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $resolved -PathType Leaf)) {
            throw "rustup could not resolve $ToolName for Rust $RequiredRustVersion"
        }
        return $resolved
    }

    $extension = if ($IsWindows) { '.exe' } else { '' }
    $candidates = Get-ChildItem -Path (Join-Path $HOME ".rustup/toolchains/$RequiredRustVersion-*/bin/$ToolName$extension") -File -ErrorAction SilentlyContinue
    $resolvedCandidate = $candidates | Select-Object -First 1
    if ($null -eq $resolvedCandidate) {
        throw "Rust $RequiredRustVersion is required. Install rustup, or install that exact toolchain and all three Android targets."
    }
    return $resolvedCandidate.FullName
}

function Resolve-NdkRoot {
    if ($NdkRoot) {
        return (Resolve-Path -LiteralPath $NdkRoot).Path
    }

    foreach ($candidate in @($env:ANDROID_NDK_ROOT, $env:ANDROID_NDK_HOME)) {
        if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Container)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $sdkRoot = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $null }
    if (-not $sdkRoot -and $IsWindows) {
        $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
    }
    if ($sdkRoot) {
        $candidate = Join-Path $sdkRoot "ndk/$RequiredNdkVersion"
        if (Test-Path -LiteralPath $candidate -PathType Container) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw "Android NDK $RequiredNdkVersion was not found. Pass -NdkRoot or set ANDROID_NDK_ROOT."
}

$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$SourceRoot = (Resolve-Path -LiteralPath $SourceDir).Path
$PatchPath = Join-Path $PSScriptRoot 'tun2proxy-drover.patch'
$LockPath = Join-Path $PSScriptRoot 'Cargo.lock'

if (-not (Test-Path -LiteralPath (Join-Path $SourceRoot '.git'))) {
    # A git worktree has a .git file instead of a directory, so test existence
    # rather than PathType here.
    throw "SourceDir is not a tun2proxy git checkout: $SourceRoot"
}

$head = (& git -C $SourceRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $head -ne $UpstreamCommit) {
    throw "Expected tun2proxy commit $UpstreamCommit, found $head"
}
$dirty = (& git -C $SourceRoot status --porcelain)
if ($LASTEXITCODE -ne 0 -or $dirty) {
    throw 'SourceDir must be a clean checkout before applying the Drover patch.'
}

$ResolvedNdkRoot = Resolve-NdkRoot
$sourceProperties = Get-Content -Raw (Join-Path $ResolvedNdkRoot 'source.properties')
if ($sourceProperties -notmatch "(?m)^Pkg\.Revision\s*=\s*$([regex]::Escape($RequiredNdkVersion))\s*$") {
    throw "Drover native releases require NDK $RequiredNdkVersion. Found: $ResolvedNdkRoot"
}

$hostTag = if ($IsWindows) {
    'windows-x86_64'
} elseif ($IsLinux) {
    'linux-x86_64'
} else {
    throw 'This build script currently supports Windows x64 and Linux x64 hosts.'
}
$toolSuffix = if ($IsWindows) { '.cmd' } else { '' }
$binarySuffix = if ($IsWindows) { '.exe' } else { '' }
$toolchainBin = Join-Path $ResolvedNdkRoot "toolchains/llvm/prebuilt/$hostTag/bin"
if (-not (Test-Path -LiteralPath $toolchainBin -PathType Container)) {
    throw "NDK host toolchain was not found: $toolchainBin"
}
$llvmObjcopy = Join-Path $toolchainBin "llvm-objcopy$binarySuffix"
$llvmStrip = Join-Path $toolchainBin "llvm-strip$binarySuffix"
foreach ($tool in @($llvmObjcopy, $llvmStrip)) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "Required NDK binary was not found: $tool"
    }
}

$Cargo = Resolve-ExactToolchainCommand cargo
$Rustc = Resolve-ExactToolchainCommand rustc
$rustVersionText = (& $Rustc --version).Trim()
if ($LASTEXITCODE -ne 0 -or $rustVersionText -notmatch "^rustc $([regex]::Escape($RequiredRustVersion))\b") {
    throw "Expected rustc $RequiredRustVersion, found: $rustVersionText"
}
$env:RUSTC = $Rustc
$rustBin = Split-Path -Parent $Rustc
$env:PATH = "$rustBin$([IO.Path]::PathSeparator)$env:PATH"

Invoke-Checked git -C $SourceRoot apply --check $PatchPath
Invoke-Checked git -C $SourceRoot apply $PatchPath
Copy-Item -LiteralPath $LockPath -Destination (Join-Path $SourceRoot 'Cargo.lock') -Force

if (-not $TargetDir) {
    $TargetDir = Join-Path $SourceRoot 'target-drover-android'
}
$TargetRoot = [System.IO.Path]::GetFullPath($TargetDir)

$env:SOURCE_DATE_EPOCH = $SourceDateEpoch.ToString([Globalization.CultureInfo]::InvariantCulture)
$env:CARGO_TARGET_DIR = $TargetRoot
$remapFlags = @(
    '-Clink-arg=-Wl,-z,max-page-size=16384'
    '-Cdebuginfo=1'
    "--remap-path-prefix=$HOME=/build/user"
    "--remap-path-prefix=$SourceRoot=/src/tun2proxy"
    "--remap-path-prefix=$TargetRoot=/build/target"
)
$env:CARGO_ENCODED_RUSTFLAGS = $remapFlags -join [char]0x1f

foreach ($target in $Targets) {
    $linker = Join-Path $toolchainBin "$($target.ClangTarget)$AndroidApi-clang$toolSuffix"
    $archiver = Join-Path $toolchainBin "llvm-ar$binarySuffix"
    if (-not (Test-Path -LiteralPath $linker -PathType Leaf)) {
        throw "NDK linker was not found: $linker"
    }
    if (-not (Test-Path -LiteralPath $archiver -PathType Leaf)) {
        throw "NDK archiver was not found: $archiver"
    }

    $cargoTarget = $target.RustTarget.ToUpperInvariant().Replace('-', '_')
    $ccTarget = $target.RustTarget.Replace('-', '_')
    [Environment]::SetEnvironmentVariable("CARGO_TARGET_${cargoTarget}_LINKER", $linker, 'Process')
    [Environment]::SetEnvironmentVariable("CC_${ccTarget}", $linker, 'Process')
    [Environment]::SetEnvironmentVariable("AR_${ccTarget}", $archiver, 'Process')
}

Push-Location $SourceRoot
try {
    # A fresh CI runner has no Cargo registry or Git checkout cache. Resolve the
    # already-locked graph online once, then keep every compile/test offline and
    # immutable with --frozen.
    Invoke-Checked $Cargo fetch --locked

    if (-not $SkipHostTests) {
        $rustfmt = & $Rustc --print sysroot
        $rustfmt = Join-Path $rustfmt "bin/rustfmt$binarySuffix"
        if (Test-Path -LiteralPath $rustfmt -PathType Leaf) {
            Invoke-Checked -Executable $Cargo -Arguments @('fmt', '--all', '--', '--check')
        } else {
            throw "rustfmt for Rust $RequiredRustVersion is required when host tests are enabled. Install the rustfmt component or pass -SkipHostTests only after formatting and tests have passed elsewhere."
        }
        Invoke-Checked -Executable $Cargo -Arguments @(
            'test',
            '--frozen',
            '--no-default-features',
            '--lib',
            'drover_tests',
            '--',
            '--test-threads=1'
        )
        Invoke-Checked $Cargo check --frozen --no-default-features --features traffic-stats --lib
    }

    foreach ($target in $Targets) {
        Invoke-Checked $Cargo build --frozen --no-default-features --target $target.RustTarget --release --lib
        $library = Join-Path $TargetRoot "$($target.RustTarget)/release/libtun2proxy.so"
        if (-not (Test-Path -LiteralPath $library -PathType Leaf)) {
            throw "Cargo completed without producing $library"
        }
        $symbolFile = Join-Path $RepoRoot "app/build/native-symbols/$($target.AndroidAbi)/libtun2proxy.so.dbg"
        New-Item -ItemType Directory -Path (Split-Path -Parent $symbolFile) -Force | Out-Null
        Invoke-Checked $llvmObjcopy --only-keep-debug $library $symbolFile
        $destination = Join-Path $RepoRoot "app/src/main/jniLibs/$($target.AndroidAbi)/libtun2proxy.so"
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $library -Destination $destination -Force
        Invoke-Checked $llvmStrip --strip-unneeded $destination
        $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
        $symbolHash = (Get-FileHash -LiteralPath $symbolFile -Algorithm SHA256).Hash.ToLowerInvariant()
        Write-Host "$($target.AndroidAbi)  $hash  $destination"
        Write-Host "$($target.AndroidAbi)-symbols  $symbolHash  $symbolFile"
    }
}
finally {
    Pop-Location
}
