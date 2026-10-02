package com.example.app_academiafr.api;

import com.example.app_academiafr.BuildConfig;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Cria o cliente da API e guarda o token do usuário logado. */
public final class ApiClient {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private static AcademiaApi api;
    private static volatile String token;
    /** Os testes no computador trocam o endereço (10.0.2.2 só existe dentro do emulador). */
    static String baseUrl = BuildConfig.API_BASE_URL;

    private ApiClient() {}

    public static void setToken(String value) { token = value; }
    public static String getToken() { return token; }

    public static synchronized AcademiaApi get() {
        if (api == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request.Builder r = chain.request().newBuilder().header("Accept", "application/json");
                        if (!BuildConfig.API_HOST.isEmpty()) r.header("Host", BuildConfig.API_HOST);
                        if (token != null && chain.request().header("Authorization") == null) r.header("Authorization", "Bearer " + token);
                        return chain.proceed(r.build());
                    })
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create(GSON))
                    .build()
                    .create(AcademiaApi.class);
        }
        return api;
    }

    /** Mensagem de erro da API ({message} ou {errors: {campo: [msg]}}), ou o texto padrão. */
    public static String errorMessage(Response<?> response, String fallback) {
        if (response.code() == 401) return "Sua sessão expirou. Entre novamente.";
        try (ResponseBody body = response.errorBody()) {
            if (body == null) return fallback;
            JsonObject json = GSON.fromJson(body.charStream(), JsonObject.class);
            if (json != null && json.has("errors") && json.get("errors").isJsonObject()) {
                for (java.util.Map.Entry<String, JsonElement> e : json.getAsJsonObject("errors").entrySet()) {
                    if (e.getValue().isJsonArray()) {
                        JsonArray list = e.getValue().getAsJsonArray();
                        if (list.size() > 0) return list.get(0).getAsString();
                    }
                }
            }
            if (json != null && json.has("message") && response.code() < 500) return json.get("message").getAsString();
        } catch (Exception ignored) {
            // Resposta que não é JSON: usa a mensagem padrão.
        }
        return fallback;
    }

    public static final String OFFLINE = "Não foi possível conectar ao servidor. Verifique sua internet e se a API está no ar.";

    /** "89.90" → "R$ 89,90". */
    public static String money(String value) {
        try {
            return String.format(java.util.Locale.forLanguageTag("pt-BR"), "R$ %,.2f", Double.parseDouble(value));
        } catch (Exception e) {
            return "R$ " + value;
        }
    }

    /** "2026-12-31" → "31/12/2026". */
    public static String date(String iso) {
        if (iso == null || iso.length() < 10) return iso == null ? "" : iso;
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }
}
