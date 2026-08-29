#include <jni.h>
#include <dlfcn.h>
#include <cstddef>
#include <cstdint>
#include <cstdlib>

namespace {

constexpr int BROTLI_DECODER_RESULT_ERROR = 0;
constexpr int BROTLI_DECODER_RESULT_SUCCESS = 1;
constexpr int BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT = 2;
constexpr int BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT = 3;
using BrotliAllocFunc = void* (*)(void*, size_t);
using BrotliFreeFunc = void (*)(void*, void*);
using BrotliDecoderCreateInstance = void* (*)(BrotliAllocFunc, BrotliFreeFunc, void*);
using BrotliDecoderDestroyInstance = void (*)(void*);
using BrotliDecoderDecompressStream = int (*)(
        void*, size_t*, const uint8_t**, size_t*, uint8_t**, size_t*);

}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_dot_gallery_core_sandbox_NativeBrotliDecoder_nativeDecompress(
        JNIEnv* env, jobject, jbyteArray input, jint max_output_size) {
    if (input == nullptr || max_output_size <= 0) return nullptr;
    const jsize input_size = env->GetArrayLength(input);
    if (input_size <= 0) return nullptr;

    void* handle = dlopen("libbrotlidec.so", RTLD_NOW | RTLD_LOCAL);
    if (handle == nullptr) return nullptr;
    auto create = reinterpret_cast<BrotliDecoderCreateInstance>(
            dlsym(handle, "BrotliDecoderCreateInstance"));
    auto destroy = reinterpret_cast<BrotliDecoderDestroyInstance>(
            dlsym(handle, "BrotliDecoderDestroyInstance"));
    auto decompress = reinterpret_cast<BrotliDecoderDecompressStream>(
            dlsym(handle, "BrotliDecoderDecompressStream"));
    if (create == nullptr || destroy == nullptr || decompress == nullptr) {
        dlclose(handle);
        return nullptr;
    }

    void* state = create(nullptr, nullptr, nullptr);
    jbyte* input_bytes = env->GetByteArrayElements(input, nullptr);
    if (state == nullptr || input_bytes == nullptr) {
        if (state != nullptr) destroy(state);
        if (input_bytes != nullptr) env->ReleaseByteArrayElements(input, input_bytes, JNI_ABORT);
        dlclose(handle);
        return nullptr;
    }

    const size_t limit = static_cast<size_t>(max_output_size);
    size_t capacity = static_cast<size_t>(input_size) * 4;
    if (capacity < 1024) capacity = 1024;
    if (capacity > limit) capacity = limit;
    auto* output = static_cast<uint8_t*>(malloc(capacity));
    if (output == nullptr) {
        destroy(state);
        env->ReleaseByteArrayElements(input, input_bytes, JNI_ABORT);
        dlclose(handle);
        return nullptr;
    }

    size_t available_input = static_cast<size_t>(input_size);
    auto* next_input = reinterpret_cast<const uint8_t*>(input_bytes);
    size_t total_output = 0;
    int result = BROTLI_DECODER_RESULT_ERROR;
    while (true) {
        size_t available_output = capacity - total_output;
        uint8_t* next_output = output + total_output;
        result = decompress(
                state,
                &available_input,
                &next_input,
                &available_output,
                &next_output,
                &total_output);
        if (result == BROTLI_DECODER_RESULT_SUCCESS) break;
        if (result == BROTLI_DECODER_RESULT_NEEDS_MORE_INPUT ||
            result == BROTLI_DECODER_RESULT_ERROR || capacity == limit) {
            break;
        }
        if (result != BROTLI_DECODER_RESULT_NEEDS_MORE_OUTPUT) break;
        const size_t next_capacity = capacity > limit / 2 ? limit : capacity * 2;
        auto* resized = static_cast<uint8_t*>(realloc(output, next_capacity));
        if (resized == nullptr) break;
        output = resized;
        capacity = next_capacity;
    }

    destroy(state);
    env->ReleaseByteArrayElements(input, input_bytes, JNI_ABORT);
    dlclose(handle);
    if (result != BROTLI_DECODER_RESULT_SUCCESS || total_output > limit) {
        free(output);
        return nullptr;
    }

    jbyteArray decoded = env->NewByteArray(static_cast<jsize>(total_output));
    if (decoded != nullptr) {
        env->SetByteArrayRegion(
                decoded,
                0,
                static_cast<jsize>(total_output),
                reinterpret_cast<const jbyte*>(output));
    }
    free(output);
    return decoded;
}
