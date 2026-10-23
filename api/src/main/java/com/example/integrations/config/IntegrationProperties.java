package com.example.integrations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    private String ssoBaseUrl;
    private String ssoAppSecret;
    private long ssoCodeTtlSeconds = 60;
    private String sfCheckWord;
    private String sfCustomerCode;

    public String getSsoBaseUrl() { return ssoBaseUrl; }
    public void setSsoBaseUrl(String ssoBaseUrl) { this.ssoBaseUrl = ssoBaseUrl; }
    public String getSsoAppSecret() { return ssoAppSecret; }
    public void setSsoAppSecret(String ssoAppSecret) { this.ssoAppSecret = ssoAppSecret; }
    public long getSsoCodeTtlSeconds() { return ssoCodeTtlSeconds; }
    public void setSsoCodeTtlSeconds(long ssoCodeTtlSeconds) { this.ssoCodeTtlSeconds = ssoCodeTtlSeconds; }
    public String getSfCheckWord() { return sfCheckWord; }
    public void setSfCheckWord(String sfCheckWord) { this.sfCheckWord = sfCheckWord; }
    public String getSfCustomerCode() { return sfCustomerCode; }
    public void setSfCustomerCode(String sfCustomerCode) { this.sfCustomerCode = sfCustomerCode; }
}
