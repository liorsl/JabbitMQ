package dev.voigon.jabbitmq;

import com.rabbitmq.stream.ConfirmationHandler;
import com.rabbitmq.stream.ConfirmationStatus;

import java.util.concurrent.CompletableFuture;

/* package-private */ class FutureConfirmationHandler implements ConfirmationHandler {

    /* package-private */ final CompletableFuture<ConfirmationStatus> future = new CompletableFuture<>();

    @Override
    public void handle(ConfirmationStatus confirmationStatus) {
        future.complete(confirmationStatus);

    }
}
