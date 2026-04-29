# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM ubuntu:24.04 AS builder

RUN apt-get update && apt-get install -y \
    git cmake build-essential curl unzip \
    openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /project
COPY . .

# Build FMIlib (C library) first, then the Kotlin/Native backend binary
RUN ./gradlew cmakeBuild
RUN ./gradlew :backend:linuxX64Binaries --no-daemon

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM ubuntu:24.04 AS runtime

RUN apt-get update && apt-get install -y \
    libstdc++6 libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the compiled native binary
COPY --from=builder /project/backend/build/bin/linuxX64/releaseExecutable/backend.kexe ./backend

# Copy the FMIlib shared library and set it up
COPY --from=builder /project/fmilib/build/fmilib-install/ ./fmilib-install/

COPY --from=builder /project/fmilib/build/fmilib-install/linux-amd64/lib/libfmilib_shared.so /usr/local/lib/
RUN ldconfig

# Make the binary executable
RUN chmod +x ./backend

# Create the runtime directories the app needs
RUN mkdir -p resources/models resources/extracted

EXPOSE 8080

# Pass /app as baseDir (args[0]) so routing resolves paths correctly
CMD ["./backend", "/app"]