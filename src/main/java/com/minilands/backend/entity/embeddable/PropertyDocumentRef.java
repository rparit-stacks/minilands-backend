package com.minilands.backend.entity.embeddable;

import com.minilands.backend.entity.enums.PropertyDocumentType;

public class PropertyDocumentRef {

    private String title;
    private String documentUrl;
    private PropertyDocumentType documentType;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public PropertyDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(PropertyDocumentType documentType) {
        this.documentType = documentType;
    }
}
