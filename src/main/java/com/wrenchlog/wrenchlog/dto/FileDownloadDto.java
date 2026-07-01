package com.wrenchlog.wrenchlog.dto;

import org.springframework.core.io.Resource;

public record FileDownloadDto(Resource resource, String contentType, String fileName) {
}