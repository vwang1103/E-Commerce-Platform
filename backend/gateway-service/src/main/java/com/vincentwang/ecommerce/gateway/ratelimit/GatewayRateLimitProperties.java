package com.vincentwang.ecommerce.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gateway.rate-limit")
public class GatewayRateLimitProperties {

    private boolean enabled = true;
    private boolean failOpen = true;
    private boolean useForwardedFor;
    private int trustedProxyCount = 1;
    private Policy general = new Policy(50, 100);
    private Policy search = new Policy(20, 40);
    private Policy orderWrite = new Policy(5, 10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public boolean isUseForwardedFor() {
        return useForwardedFor;
    }

    public void setUseForwardedFor(boolean useForwardedFor) {
        this.useForwardedFor = useForwardedFor;
    }

    public int getTrustedProxyCount() {
        return trustedProxyCount;
    }

    public void setTrustedProxyCount(int trustedProxyCount) {
        if (trustedProxyCount < 1) {
            throw new IllegalArgumentException("trustedProxyCount must be at least 1");
        }
        this.trustedProxyCount = trustedProxyCount;
    }

    public Policy getGeneral() {
        return general;
    }

    public void setGeneral(Policy general) {
        this.general = general;
    }

    public Policy getSearch() {
        return search;
    }

    public void setSearch(Policy search) {
        this.search = search;
    }

    public Policy getOrderWrite() {
        return orderWrite;
    }

    public void setOrderWrite(Policy orderWrite) {
        this.orderWrite = orderWrite;
    }

    public static final class Policy {

        private int replenishRate;
        private long burstCapacity;

        public Policy() {}

        public Policy(int replenishRate, long burstCapacity) {
            setReplenishRate(replenishRate);
            setBurstCapacity(burstCapacity);
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            if (replenishRate < 1) {
                throw new IllegalArgumentException("replenishRate must be at least 1");
            }
            this.replenishRate = replenishRate;
        }

        public long getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(long burstCapacity) {
            if (burstCapacity < 1) {
                throw new IllegalArgumentException("burstCapacity must be at least 1");
            }
            this.burstCapacity = burstCapacity;
        }
    }
}
