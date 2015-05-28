__kernel void vectorMatrix(
    __global const float *a,
    __global const float *x,
    __global float *ax ) {

    int gid = get_global_id( 0 );
    ax[ gid ] = 0;
    for ( int i = 0; i < 5; i++ ) {
        ax[ gid ] += a[ gid * 5 + i ] * x[ i ];
    }
}
