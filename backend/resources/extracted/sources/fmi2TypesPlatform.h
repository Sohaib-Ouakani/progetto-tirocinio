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