package app.drover.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Loads legal notices only when the user explicitly opens this screen. */
public final class ThirdPartyLicensesActivity extends Activity {
    private static final String LICENSE_ASSET = "third_party_licenses.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third_party_licenses);

        TextView licenseText = findViewById(R.id.license_text);
        licenseText.setTextIsSelectable(true);
        licenseText.setText(loadLicenses());

        Button closeButton = findViewById(R.id.close_licenses_button);
        closeButton.setOnClickListener(view -> finish());
    }

    private String loadLicenses() {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[8192];
        try (Reader reader = new InputStreamReader(
                getAssets().open(LICENSE_ASSET),
                StandardCharsets.UTF_8
        )) {
            int length;
            while ((length = reader.read(buffer)) != -1) {
                text.append(buffer, 0, length);
            }
            return text.toString();
        } catch (IOException error) {
            return getString(R.string.licenses_load_failed, error.getMessage());
        }
    }
}
