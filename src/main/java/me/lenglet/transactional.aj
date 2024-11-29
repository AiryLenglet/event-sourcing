import static me.lenglet.ConnectionFactory.wrapInTransaction;

aspect Transactional {

    pointcut transactionalMethod(): execution(@me.lenglet.Transactional * *(..));

    Object around(): transactionalMethod() {
        return wrapInTransaction(() -> proceed());
    }
}