package com.utp.roggerexpress.exception;

/** Se lanza cuando una cuenta acumula demasiados intentos de acceso fallidos. */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosRestantes;

    public DemasiadosIntentosException(long segundosRestantes) {
        super("Demasiados intentos fallidos. Vuelva a intentarlo en "
                + Math.max(1, segundosRestantes / 60) + " minuto(s).");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
