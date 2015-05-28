package matrix;


import org.jocl.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import static org.jocl.CL.*;

public class VectorMatrix {

	public static void main( final String... args ) {

		// Size of matrix
		final int n = 8;
		final int m = 5;

		// Set seed to ensure same values
		final Random random = new Random( 2015 );

		// Generate vector x
		final float[] x = new float[ m ];
		for ( int i = 0; i < m; i++ ) {
			x[ i ] = random.nextFloat();
		}

		// Generate matrix a
		final float[] a = new float[ m * n ];
		for ( int i = 0; i < m * n; i++ ) {
			a[ i ] = random.nextFloat();
		}

		// Buffer for ax
		final float[] ax = new float[ n ];

		System.out.print( "A: " );
		System.out.println( Arrays.toString( a ) );
		System.out.print( "x: " );
		System.out.println( Arrays.toString( x ) );

		// Create some pointers
		final Pointer aPointer = Pointer.to( a );
		final Pointer xPointer = Pointer.to( x );
		final Pointer axPointer = Pointer.to( ax );

		// Number of platforms
		final int numOfPlatforms[] = new int[ 1 ];
		clGetPlatformIDs( 0, null, numOfPlatforms );
		System.out.println( "Platforms: " + numOfPlatforms[ 0 ] );

		// Grab the platforms
		final cl_platform_id platforms[] = new cl_platform_id[ numOfPlatforms[ 0 ] ];
		clGetPlatformIDs( numOfPlatforms[ 0 ], platforms, null );

		// Use the first platform
		final cl_platform_id platform = platforms[ 0 ];

		// Create the context properties
		final cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty( CL_CONTEXT_PLATFORM, platform );

		// Number of devices
		int numOfDevices[] = new int[ 1 ];
		clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 0, null, numOfDevices );
		System.out.println( "Devices: " + numOfDevices[ 0 ] );

		// Grab the devices
		final cl_device_id devices[] = new cl_device_id[ numOfDevices[ 0 ] ];
		clGetDeviceIDs( platform, CL_DEVICE_TYPE_ALL, numOfDevices[ 0 ], devices, null );

		// Use the first device
		final cl_device_id device = devices[ 0 ];

		// Create a context for the device
		final cl_context context = clCreateContext( contextProperties, 1, new cl_device_id[]{ device }, null, null, null );

		// Create a command queue for the device
		final cl_command_queue commandQueue = clCreateCommandQueue( context, device, 0, null );

		// Read in the program source
		final String programSource = new BufferedReader( new InputStreamReader(
			VectorMatrix.class.getResourceAsStream( "matrix.cl" )
		) ).lines().parallel().collect( Collectors.joining( "\n" ) );

		// Create the program from the source code
		final cl_program program = clCreateProgramWithSource( context, 1, new String[]{ programSource }, null, null );
		clBuildProgram( program, 0, null, null, null, null );

		// Create the kernel
		cl_kernel kernel = clCreateKernel( program, "vectorMatrix", null );

		// Create memory objects
		cl_mem memObjects[] = new cl_mem[ 3 ];
		memObjects[ 0 ] = clCreateBuffer( context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * n * m, aPointer, null );
		memObjects[ 1 ] = clCreateBuffer( context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * m, xPointer, null );
		memObjects[ 2 ] = clCreateBuffer( context, CL_MEM_READ_WRITE, Sizeof.cl_float * n, null, null );

		// Kernel arguments
		clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( memObjects[ 0 ] ) );
		clSetKernelArg( kernel, 1, Sizeof.cl_mem, Pointer.to( memObjects[ 1 ] ) );
		clSetKernelArg( kernel, 2, Sizeof.cl_mem, Pointer.to( memObjects[ 2 ] ) );

		// Work item dim
		final long global_work_size[] = new long[]{ n };
		final long local_work_size[] = new long[]{ 1 };

		// Execute the kernel
		clEnqueueNDRangeKernel( commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null );

		// Read the output data
		clEnqueueReadBuffer( commandQueue, memObjects[ 2 ], CL_TRUE, 0, n * Sizeof.cl_float, axPointer, 0, null, null );

		System.out.println( "Ax: " + Arrays.toString( ax ) );
	}
}
