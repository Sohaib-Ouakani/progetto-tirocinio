package utility

/**
 * Synthesizes FMI (Functional Mock-up Interface) header files required for compilation.
 * Creates the necessary header files that define FMI types, functions, and function signatures.
 *
 * @param fs The [FilesystemManager] instance used to write header files to disk.
 */
class FmiHeaderSynthesiser(private val fs: FilesystemManager) {

    /**
     * Generates and writes FMI header files to the specified directory.
     * Creates fmi2FunctionTypes.h, fmi2Functions.h, and fmi2TypesPlatform.h files.
     *
     * @param dir The directory where the header files will be written.
     */
    fun synthesise(dir: String) {
        fs.writeFile("$dir/fmi2FunctionTypes.h", FMI2_FUNCTION_TYPES)
        fs.writeFile("$dir/fmi2Functions.h", FMI2_FUNCTIONS)
        fs.writeFile("$dir/fmi2TypesPlatform.h", FMI2_TYPES_PLATFORM)
    }

    /**
     * Companion object containing the predefined FMI 2.0 header file contents.
     * These constants define the standard C header files required for FMI-compliant compilation.
     */
    companion object {
        private val FMI2_FUNCTION_TYPES = """
        #ifndef fmi2FunctionTypes_h
        #define fmi2FunctionTypes_h
        #include <stdlib.h>
        #include "fmi2TypesPlatform.h"
        typedef int fmi2Status;
        #define fmi2OK      0
        #define fmi2Warning 1
        #define fmi2Discard 2
        #define fmi2Error   3
        #define fmi2Fatal   4
        #define fmi2Pending 5
        typedef int fmi2Type;
        #define fmi2ModelExchange 0
        #define fmi2CoSimulation  1
        typedef int fmi2StatusKind;
        #define fmi2DoStepStatus       0
        #define fmi2PendingStatus      1
        #define fmi2LastSuccessfulTime 2
        #define fmi2Terminated         3
        typedef struct {
          fmi2Boolean newDiscreteStatesNeeded;
          fmi2Boolean terminateSimulation;
          fmi2Boolean nominalsOfContinuousStatesChanged;
          fmi2Boolean valuesOfContinuousStatesChanged;
          fmi2Boolean nextEventTimeDefined;
          fmi2Real    nextEventTime;
        } fmi2EventInfo;
        #endif
        """.trimIndent()

        private val FMI2_FUNCTIONS = """
        #ifndef fmi2Functions_h
        #define fmi2Functions_h
        #include <stdlib.h>
        #include "fmi2TypesPlatform.h"
        #include "fmi2FunctionTypes.h"
        #define fmi2Version "2.0"
        #ifndef FMI2_Export
          #if defined _WIN32 || defined __CYGWIN__
            #define FMI2_Export __declspec(dllexport)
          #else
            #define FMI2_Export __attribute__((visibility("default")))
          #endif
        #endif
        #endif
        """.trimIndent()

        private val FMI2_TYPES_PLATFORM = """
        #ifndef fmi2TypesPlatform_h
        #define fmi2TypesPlatform_h
        #include <stddef.h>
        #define fmi2TypesPlatform "default"
        typedef double           fmi2Real;
        typedef int              fmi2Integer;
        typedef int              fmi2Boolean;
        typedef char             fmi2Char;
        typedef const fmi2Char*  fmi2String;
        typedef char             fmi2Byte;
        #define fmi2True  1
        #define fmi2False 0
        typedef void* fmi2Component;
        typedef void* fmi2ComponentEnvironment;
        typedef void* fmi2FMUstate;
        typedef unsigned int fmi2ValueReference;
        typedef void  (*fmi2CallbackLogger)(fmi2ComponentEnvironment,fmi2String,int,fmi2String,fmi2String,...);
        typedef void* (*fmi2CallbackAllocateMemory)(size_t,size_t);
        typedef void  (*fmi2CallbackFreeMemory)(void*);
        typedef void  (*fmi2StepFinished)(fmi2ComponentEnvironment,int);
        typedef struct {
          fmi2CallbackLogger         logger;
          fmi2CallbackAllocateMemory allocateMemory;
          fmi2CallbackFreeMemory     freeMemory;
          fmi2StepFinished           stepFinished;
          fmi2ComponentEnvironment   componentEnvironment;
        } fmi2CallbackFunctions;
        #endif
        """.trimIndent()
    }
}
