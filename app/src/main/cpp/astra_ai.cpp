#include <jni.h>
#include <string>
#include <vector>
#include <cstdio>
#include <ctime>
#include <algorithm>
#include <sys/stat.h>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "astra_ai"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct NativeEngine {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;

    bool initialized = false;
    bool loaded = false;
};

extern "C" {

// ============================================================
// GET NATIVE ENGINE
// ============================================================

static NativeEngine* getEngine(JNIEnv* env, jobject obj) {

    jclass cls = env->GetObjectClass(obj);

    if (cls == nullptr) {
        return nullptr;
    }

    jfieldID fieldId =
            env->GetFieldID(cls, "nativeHandle", "J");

    if (fieldId == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    jlong handle =
            env->GetLongField(obj, fieldId);

    return reinterpret_cast<NativeEngine*>(handle);
}


// ============================================================
// SET NATIVE ENGINE
// ============================================================

static void setEngine(
        JNIEnv* env,
        jobject obj,
        NativeEngine* engine
) {

    jclass cls = env->GetObjectClass(obj);

    if (cls == nullptr) {
        return;
    }

    jfieldID fieldId =
            env->GetFieldID(cls, "nativeHandle", "J");

    if (fieldId == nullptr) {
        env->ExceptionClear();
        return;
    }

    env->SetLongField(
            obj,
            fieldId,
            reinterpret_cast<jlong>(engine)
    );
}


// ============================================================
// INITIALIZE ENGINE
// ============================================================

JNIEXPORT void JNICALL
Java_com_example_astra_ai_LocalAI_nativeInit(
        JNIEnv* env,
        jobject obj
) {
    llama_backend_init();

    NativeEngine* engine =
            new NativeEngine();

    if (engine == nullptr) {
        LOGE("Failed to allocate NativeEngine");
        return;
    }

    engine->initialized = true;

    setEngine(env, obj, engine);

    LOGI("Native engine initialized and backend ready");
}


// ============================================================
// LOAD MODEL
// ============================================================

JNIEXPORT jboolean JNICALL
Java_com_example_astra_ai_LocalAI_nativeLoadModel(
        JNIEnv* env,
        jobject obj,
        jstring path
) {

    if (path == nullptr) {
        LOGE("Model path is null");
        return JNI_FALSE;
    }

    NativeEngine* engine =
            getEngine(env, obj);

    if (engine == nullptr ||
        !engine->initialized) {

        LOGE("Engine is not initialized before model load");

        return JNI_FALSE;
    }

    if (engine->model != nullptr ||
        engine->ctx != nullptr) {

        LOGE("Engine already has a model/context loaded");

        return JNI_FALSE;
    }


    // --------------------------------------------------------
    // GET MODEL PATH
    // --------------------------------------------------------

    const char* modelPath =
            env->GetStringUTFChars(path, nullptr);

    if (modelPath == nullptr) {

        LOGE("Unable to read model path string");

        return JNI_FALSE;
    }

    LOGI(
            "JNI loadModel path: %s",
            modelPath
    );

    FILE* file =
            fopen(modelPath, "rb");

    if (file == nullptr) {
        LOGE("JNI file check failed: cannot open model file. Path invalid or file missing: %s", modelPath);
        env->ReleaseStringUTFChars(path, modelPath);
        return JNI_FALSE;
    }

    struct stat fileStat{};
    if (stat(modelPath, &fileStat) == 0) {
        LOGI("Model file exists. Size bytes: %lld", (long long)fileStat.st_size);
    } else {
        LOGE("Model file stat failed for path: %s", modelPath);
    }
    fclose(file);

    LOGI("Model file opened successfully");

    llama_model_params modelParams =
            llama_model_default_params();

    LOGI("Calling llama_model_load_from_file with path: %s", modelPath);

    llama_model* model =
            llama_model_load_from_file(
                    modelPath,
                    modelParams
            );


    // Release Java string ONLY ONCE
    env->ReleaseStringUTFChars(
            path,
            modelPath
    );


    if (model == nullptr) {
        LOGE("llama_model_load_from_file FAILED for path: %s", modelPath);
        env->ReleaseStringUTFChars(path, modelPath);
        return JNI_FALSE;
    }

    LOGI("llama_model_load_from_file succeeded for: %s", modelPath);


    // --------------------------------------------------------
    // CREATE LLAMA CONTEXT
    // --------------------------------------------------------

    llama_context_params ctxParams =
            llama_context_default_params();

    // Set a reasonable context size for mobile (e.g. 2048)
    // Qwen 2.5 default might be too large for emulator RAM
    ctxParams.n_ctx = 2048;

    // Use 4 threads for mobile performance as requested
    ctxParams.n_threads = 4;
    ctxParams.n_threads_batch = 4;

    llama_context* ctx =
            llama_init_from_model(
                    model,
                    ctxParams
            );

    if (ctx == nullptr) {

        LOGE(
                "Failed to create llama context"
        );

        llama_model_free(model);

        return JNI_FALSE;
    }


    // --------------------------------------------------------
    // SAVE MODEL AND CONTEXT
    // --------------------------------------------------------

    engine->model = model;
    engine->ctx = ctx;

    engine->loaded = true;

    LOGI(
            "Model and context loaded successfully"
    );

    return JNI_TRUE;
}


// ============================================================
// GENERATE RESPONSE
// CURRENTLY TEST/PLACEHOLDER VERSION
// ============================================================

JNIEXPORT jstring JNICALL
Java_com_example_astra_ai_LocalAI_nativeGenerate(
        JNIEnv* env,
        jobject obj,
        jstring prompt
) {
    NativeEngine* engine = getEngine(env, obj);

    if (engine == nullptr || !engine->initialized || !engine->loaded || engine->ctx == nullptr) {
        LOGE("Native model not ready for generation");
        return env->NewStringUTF("Native AI is not initialized yet.");
    }

    if (prompt == nullptr) {
        LOGE("Prompt is null");
        return env->NewStringUTF("Prompt is empty.");
    }

    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    if (promptChars == nullptr) {
        LOGE("Unable to read prompt string");
        return env->NewStringUTF("Prompt could not be processed.");
    }

    std::string userPrompt(promptChars);
    env->ReleaseStringUTFChars(prompt, promptChars);

    LOGI("Native model ready for generation. Prompt: %s", userPrompt.c_str());

    // 1. Prepare Chat Template
    const struct llama_vocab* vocab = llama_model_get_vocab(engine->model);
    std::vector<llama_chat_message> messages = {
        {"system", "You are ASTRA, a helpful AI assistant."},
        {"user", userPrompt.c_str()}
    };

    const char* tmpl = llama_model_chat_template(engine->model, nullptr);
    if (tmpl == nullptr) {
        LOGE("Model has no chat template");
        return env->NewStringUTF("Error: Model missing chat template.");
    }

    std::vector<char> formatted(2048);
    int32_t res = llama_chat_apply_template(tmpl, messages.data(), messages.size(), true, formatted.data(), formatted.size());
    if (res > (int32_t)formatted.size()) {
        formatted.resize(res);
        res = llama_chat_apply_template(tmpl, messages.data(), messages.size(), true, formatted.data(), formatted.size());
    }
    if (res < 0) {
        LOGE("Failed to apply chat template: %d", res);
        return env->NewStringUTF("Error formatting chat prompt.");
    }
    std::string formattedPrompt(formatted.data(), res);
    LOGI("Formatted prompt length: %zu", formattedPrompt.size());

    // 2. Tokenize
    std::vector<llama_token> tokens(formattedPrompt.size() + 32);
    int32_t n_tokens = llama_tokenize(vocab, formattedPrompt.c_str(), formattedPrompt.size(), tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, formattedPrompt.c_str(), formattedPrompt.size(), tokens.data(), tokens.size(), true, true);
    }
    tokens.resize(n_tokens);
    LOGI("Prompt token count: %d", (int)tokens.size());

    // 3. KV Cache Diagnostics & Reset
    uint32_t n_ctx = llama_n_ctx(engine->ctx);
    uint32_t n_batch_limit = llama_n_batch(engine->ctx);
    LOGI("Context size: %u, Batch limit: %u", n_ctx, n_batch_limit);

    if ((uint32_t)n_tokens > n_ctx) {
        LOGE("Prompt too long for context size!");
        return env->NewStringUTF("Error: Prompt exceeds context length.");
    }

    llama_memory_t mem = llama_get_memory(engine->ctx);
    if (mem != nullptr) {
        bool rm_res = llama_memory_seq_rm(mem, 0, 0, -1);
        LOGI("KV cache cleared for seq 0: %s", rm_res ? "success" : "failed");
    } else {
        LOGE("KV cache memory is NULL");
    }

    // 4. Initial Decode (Prompt)
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    batch.n_tokens = (int32_t)tokens.size();

    for (int32_t i = 0; i < batch.n_tokens; i++) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == batch.n_tokens - 1);
    }

    LOGI("Decoding prompt batch: n_tokens=%d", batch.n_tokens);

    int32_t decode_res = llama_decode(engine->ctx, batch);
    if (decode_res != 0) {
        LOGE("llama_decode failed for prompt with code: %d", decode_res);
        llama_batch_free(batch);
        return env->NewStringUTF("Error evaluating prompt.");
    }
    llama_batch_free(batch);

    // 5. Sampler Setup
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.1f, 0.0f, 0.0f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist((uint32_t)time(NULL)));

    // 6. Generation Loop
    std::string response = "";
    int32_t n_cur = tokens.size();
    int32_t n_decode = 0;
    const int32_t n_max = 256;

    LOGI("Beginning generation");

    llama_batch gen_batch = llama_batch_init(1, 0, 1);
    gen_batch.n_tokens = 1;

    while (n_decode < n_max) {
        llama_token id = llama_sampler_sample(smpl, engine->ctx, -1);

        if (llama_vocab_is_eog(vocab, id)) {
            LOGI("EOS/EOG reached");
            break;
        }

        char buf[128];
        int n = llama_token_to_piece(vocab, id, buf, sizeof(buf), 0, false);
        if (n > 0) {
            response.append(buf, n);
        }

        n_decode++;

        // Prepare next token for decode
        gen_batch.token[0] = id;
        gen_batch.pos[0] = n_cur;
        gen_batch.n_seq_id[0] = 1;
        gen_batch.seq_id[0][0] = 0;
        gen_batch.logits[0] = true;

        if (llama_decode(engine->ctx, gen_batch) != 0) {
            LOGE("llama_decode failed during generation");
            break;
        }
        n_cur++;
    }

    llama_batch_free(gen_batch);

    LOGI("Generation completed. Generated tokens: %d", n_decode);

    llama_sampler_free(smpl);

    return env->NewStringUTF(response.c_str());
}


// ============================================================
// CLEANUP
// ============================================================

JNIEXPORT void JNICALL
Java_com_example_astra_ai_LocalAI_nativeCleanup(
        JNIEnv* env,
        jobject obj
) {

    NativeEngine* engine =
            getEngine(env, obj);

    if (engine == nullptr) {
        return;
    }


    // Free context first

    if (engine->ctx != nullptr) {

        llama_free(engine->ctx);

        engine->ctx = nullptr;
    }


    // Free model

    if (engine->model != nullptr) {

        llama_model_free(
                engine->model
        );

        engine->model = nullptr;
    }


    engine->loaded = false;

    engine->initialized = false;


    delete engine;


    setEngine(
            env,
            obj,
            nullptr
    );


    LOGI(
            "Native engine cleaned up"
    );
}

} // extern "C"