package com.example.oauthjwt.dto;

public interface OAuth2Response {
    String getProvider();
    String getProviderId();
    String getEmail();
    String getName();
}
