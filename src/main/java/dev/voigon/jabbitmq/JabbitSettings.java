package dev.voigon.jabbitmq;

public record JabbitSettings(boolean autoCloseWhenUnregisteredListener) {

    public static class Builder {

        /**
         * If true, {@link JabbitEventStream} will automatically close the consumer when no more listeners are registered.
         * default is true.
         */
        private boolean autoCloseWhenUnregisteredListener = true;

        public Builder autoCloseWhenUnregisteredListener(boolean autoCloseWhenUnregisteredListener) {
            this.autoCloseWhenUnregisteredListener = autoCloseWhenUnregisteredListener;
            return this;
        }

        public JabbitSettings build() {
            return new JabbitSettings(autoCloseWhenUnregisteredListener);
        }

    }

}
