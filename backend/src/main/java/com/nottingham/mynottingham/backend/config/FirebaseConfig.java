package com.nottingham.mynottingham.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Configuration
 *
 * 初始化 Firebase Admin SDK，使后端能够访问 Firebase Realtime Database
 *
 * 前提条件：
 * 1. 在 Firebase Console 获取 Service Account Key (JSON 文件)
 * 2. 将 serviceAccountKey.json 放入 src/main/resources/ 目录
 * 3. 配置正确的 Database URL
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    // Firebase Realtime Database URL - 请根据您的项目修改
    // 格式：https://[PROJECT-ID]-default-rtdb.[REGION].firebasedatabase.app
    private static final String DATABASE_URL = "https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app";

    /**
     * 初始化 Firebase App
     * 使用 Service Account Key 进行身份验证
     */
    @Bean
    public FirebaseApp firebaseApp() {
        try {
            // 检查是否已经初始化
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase App already initialized");
                return FirebaseApp.getInstance();
            }

            // 从 resources 目录读取 Service Account Key
            InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();

            // 配置 Firebase Options
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();

            // 初始化 Firebase App
            FirebaseApp app = FirebaseApp.initializeApp(options);

            log.info("✅ Firebase Admin SDK initialized successfully");
            log.info("📊 Database URL: {}", DATABASE_URL);

            return app;

        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase Admin SDK", e);
            log.error("Please ensure serviceAccountKey.json is placed in src/main/resources/");
            throw new RuntimeException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * 提供 Firebase Database 实例
     * 用于在 Service 中直接注入使用
     */
    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }
}
