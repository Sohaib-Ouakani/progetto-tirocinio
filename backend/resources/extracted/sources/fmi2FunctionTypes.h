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