package io.searchbox.indices.mapping;


import io.searchbox.action.GenericResultAbstractAction;

/**
 * @author Dogukan Sonmez
 * @author François Thareau
 */
public class DeleteMapping extends GenericResultAbstractAction {

    protected DeleteMapping(Builder builder) {
        super(builder);

        this.indexName = builder.index;
        this.typeName = builder.type;
        setURI(buildURI());
    }

    @Override
    protected String buildURI() {
        return super.buildURI() + "/_mapping";
    }

    @Override
    public String getRestMethodName() {
        return "DELETE";
    }

    public static class Builder extends GenericResultAbstractAction.Builder<DeleteMapping, Builder> {
        private String index;
        private String type;

        public Builder(String index, String type) {
            this.index = index;
            this.type = type;
        }

        @Override
        public DeleteMapping build() {
            return new DeleteMapping(this);
        }
    }
}
