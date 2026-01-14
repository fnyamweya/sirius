package com.sirius.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sirius")
public class SiriusSecurityProperties {

    private Market market = new Market();
    private Security security = new Security();

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class Market {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Security {
        private Claims claims = new Claims();

        public Claims getClaims() {
            return claims;
        }

        public void setClaims(Claims claims) {
            this.claims = claims;
        }

        public static class Claims {
            private String marketId = "market_id";
            private String orgId = "org_id";
            private String legalEntities = "legal_entities";

            public String getMarketId() {
                return marketId;
            }

            public void setMarketId(String marketId) {
                this.marketId = marketId;
            }

            public String getOrgId() {
                return orgId;
            }

            public void setOrgId(String orgId) {
                this.orgId = orgId;
            }

            public String getLegalEntities() {
                return legalEntities;
            }

            public void setLegalEntities(String legalEntities) {
                this.legalEntities = legalEntities;
            }
        }
    }
}
