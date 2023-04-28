// Run dist check or similar
def call() {
    sh 'make distcheck DISTCHECK_CONFIGURE_FLAGS="PKG_CONFIG_PATH=/srv/knet/origin/main/lib/pkgconfig/"'
}
