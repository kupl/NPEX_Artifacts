package io.searchbox.snapshot;

/**
 * @author happyprg(hongsgo@gmail.com)
 */
public class RestoreSnapshot extends AbstractSnapshotAction {

    protected RestoreSnapshot(Builder builder) {
        super(builder);
        this.payload = builder.settings;
    }

    @Override
    protected String buildURI() {
        return super.buildURI() + "/_restore";
    }

    @Override
    public String getRestMethodName() {
        return "POST";
    }

    public static class Builder extends AbstractSnapshotAction.SingleSnapshotBuilder<RestoreSnapshot, Builder> {
        private Object settings;

        public Builder(String repository, String snapshot) {
            super(repository, snapshot);
        }

        public Builder settings(Object settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public RestoreSnapshot build() {
            return new RestoreSnapshot(this);
        }
    }
}
