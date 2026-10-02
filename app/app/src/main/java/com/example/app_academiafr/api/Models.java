package com.example.app_academiafr.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Formato dos JSONs da API. Os nomes em snake_case da API (ex.: rest_seconds)
 * viram camelCase aqui (restSeconds) pela configuração do Gson em ApiClient.
 * Valores decimais (preço, peso) chegam como texto, ex.: "89.90".
 */
public final class Models {
    private Models() {}

    public static class LoginRequest {
        final String login, password, deviceName;
        public LoginRequest(String login, String password, String deviceName) {
            this.login = login; this.password = password; this.deviceName = deviceName;
        }
    }

    public static class LoginResponse {
        public String token;
        public User user;
    }

    public static class User {
        public long id;
        public String name, email, username;
        public boolean isGlobalAdmin;
        public List<Gym> gyms = new ArrayList<>();

        public String firstName() {
            String n = name == null ? "" : name.trim();
            int space = n.indexOf(' ');
            return space > 0 ? n.substring(0, space) : n;
        }
    }

    public static class Gym {
        public long id;
        public String name, status;
        public List<String> roles = new ArrayList<>();
    }

    public static class WorkoutPlan {
        public long id;
        public String name, goal, status, scheduleDescription, publishedAt;
        public Gym gym;
        public List<WorkoutDay> days = new ArrayList<>();

        public List<WorkoutExercise> allExercises() {
            List<WorkoutExercise> all = new ArrayList<>();
            for (WorkoutDay d : days) all.addAll(d.exercises);
            return all;
        }
    }

    public static class WorkoutDay {
        public long id;
        public String label, title;
        public Integer estimatedMinutes;
        public List<WorkoutExercise> exercises = new ArrayList<>();
    }

    public static class WorkoutExercise {
        public long id;
        public int sets;
        public Integer repetitionsMin, repetitionsMax, restSeconds;
        public String targetWeightKg, notes;
        public Exercise exercise;

        /** "12" ou "10–12". */
        public String reps() {
            if (repetitionsMin == null) return "—";
            if (repetitionsMax == null || repetitionsMax.equals(repetitionsMin)) return String.valueOf(repetitionsMin);
            return repetitionsMin + "–" + repetitionsMax;
        }
    }

    public static class Exercise {
        public long id;
        public String name, muscleGroup, videoUrl, imagePath;
    }

    public static class Membership {
        public long id;
        public String status, startsOn, endsOn, contractedPrice;
        public Gym gym;
        public Plan plan;
    }

    public static class Plan {
        public long id;
        public String name, description, billingPeriod, price;
        public List<PlanFeature> features = new ArrayList<>();
    }

    public static class PlanFeature {
        public String description;
    }
}
