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