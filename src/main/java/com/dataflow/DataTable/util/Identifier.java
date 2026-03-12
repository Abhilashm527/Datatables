package com.dataflow.DataTable.util;

import com.dataflow.DataTable.enums.EnumIdentifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;

import java.io.Serializable;
import java.util.UUID;

public class Identifier implements Serializable {
    private static final long serialVersionUID = 2442778896121136685L;
    private Long id;
    private Long foreignKey;
    private String word;
    private Long localeCd;
    private UUID uuid;
    private HttpHeaders headers;
    private Pageable pageable;
    private Identifier parentIdentifier;

    public Identifier() {
    }

    public Identifier(Long attribute, EnumIdentifier enumIdentifier) {
        switch (enumIdentifier) {
            case ID -> this.id = attribute;
            case FOREIGNKEY -> this.foreignKey = attribute;
            case LOCALECD -> this.localeCd = attribute;
        }
    }

    public Identifier(String word) {
        this.word = word;
    }

    public Identifier(UUID uuid) {
        this.uuid = uuid;
    }

    public Identifier(Long id, Long localeCd) {
        this.id = id;
        this.localeCd = localeCd;
    }

    public Identifier(Long id, Long foreignKey, Long localeCd) {
        this(id, localeCd);
        this.foreignKey = foreignKey;
    }

    public Identifier(Long id, String word) {
        this.id = id;
        this.word = word;
    }

    public Identifier(Long id, HttpHeaders headers) {
        this.id = id;
        this.headers = headers;
    }

    public Identifier(String word, Long localeCd) {
        this.word = word;
        this.localeCd = localeCd;
    }

    public Identifier(Long id, String word, Long localeCd) {
        this(id, localeCd);
        this.word = word;
    }

    public Identifier(Long id, Long foreignKey, String word, Long localeCd) {
        this(id, foreignKey, localeCd);
        this.word = word;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(Long foreignKey) {
        this.foreignKey = foreignKey;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Long getLocaleCd() {
        return localeCd;
    }

    public void setLocaleCd(Long localeCd) {
        this.localeCd = localeCd;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public Pageable getPageable() {
        return pageable;
    }

    public void setPageable(Pageable pageable) {
        this.pageable = pageable;
    }

    public Identifier getParentIdentifier() {
        return parentIdentifier;
    }

    public void setParentIdentifier(Identifier parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    public static IdentifierBuilder builder() {
        return new IdentifierBuilder();
    }

    public static class IdentifierBuilder {
        private Long id;
        private Long foreignKey;
        private String word;
        private Long localeCd;
        private UUID uuid;
        private HttpHeaders headers;
        private Pageable pageable;
        private Identifier parentIdentifier;

        IdentifierBuilder() {
        }

        public IdentifierBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IdentifierBuilder foreignKey(Long foreignKey) {
            this.foreignKey = foreignKey;
            return this;
        }

        public IdentifierBuilder word(String word) {
            this.word = word;
            return this;
        }

        public IdentifierBuilder localeCd(Long localeCd) {
            this.localeCd = localeCd;
            return this;
        }

        public IdentifierBuilder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public IdentifierBuilder headers(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        public IdentifierBuilder pageable(Pageable pageable) {
            this.pageable = pageable;
            return this;
        }

        public IdentifierBuilder parentIdentifier(Identifier parentIdentifier) {
            this.parentIdentifier = parentIdentifier;
            return this;
        }

        public Identifier build() {
            Identifier identifier = new Identifier();
            identifier.setId(id);
            identifier.setForeignKey(foreignKey);
            identifier.setWord(word);
            identifier.setLocaleCd(localeCd);
            identifier.setUuid(uuid);
            identifier.setHeaders(headers);
            identifier.setPageable(pageable);
            identifier.setParentIdentifier(parentIdentifier);
            return identifier;
        }
    }
}
