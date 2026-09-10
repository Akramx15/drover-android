#ifndef DROVER_TUN2PROXY_H
#define DROVER_TUN2PROXY_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*tun2proxy_ready_callback_t)(void *context);

int32_t tun2proxy_run_with_cli_args(
    const char *cli_args,
    uint16_t tun_mtu,
    bool packet_information
);
int32_t tun2proxy_run_with_cli_args_and_ready(
    const char *cli_args,
    uint16_t tun_mtu,
    bool packet_information,
    tun2proxy_ready_callback_t ready_callback,
    void *ready_context
);
int32_t tun2proxy_stop(void);

#ifdef __cplusplus
}
#endif

#endif
