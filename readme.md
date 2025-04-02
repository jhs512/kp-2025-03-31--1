# 1강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/406a92efaa5577ad21b1abb3481d401748b2d59a)
- 프로젝트 생성
- 그록API 키 생성, 메시지 생성
  - [https://console.groq.com](https://console.groq.com)
  - [https://console.groq.com/docs/models](https://console.groq.com/docs/models)
- org.springframework.ai:spring-ai-openai-spring-boot-starter 라이브러리 추가
- 스트리밍 방식이 아닌 단순형태의 LLM API 호출

## build.gradle.kts
```kts
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}
 
group = "com"
version = "0.0.1-SNAPSHOT"
 
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
 
repositories {
    mavenCentral()
}
 
extra["springAiVersion"] = "1.0.0-M6"
 
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    implementation("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
 
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}
 
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
 
tasks.withType<Test> {
    useJUnitPlatform()
}
 
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin")) // 메인 소스 디렉토리
        }
    }

    test {
        java {
            setSrcDirs(listOf("src/test/kotlin")) // 테스트 소스 디렉토리
        }
    }
}
```

## src/main/kotlin/com/back/AIChatController.java
```java
package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AIChatController {
    private final OpenAiChatModel chatClient;

    @GetMapping("/generate")
    public String generate(
            @RequestParam(
                    value = "message",
                    defaultValue = "Tell me a joke"
            )
            String message
    ) {
        return chatClient
                .call(message);
    }
}
```

# 2강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/dbadf1314bcf28610cb485591c1bdcca07f20cc6)
- API 호출 시 응답을 스트리밍으로 받기

# 3강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/dd1d72a1c55a7d096839488b269aeb7728c2ffb7)
- 타임리프 추가, 채팅 UI 구현
- 기억력 없음
- 이전대화 저장기능 없음

# 4강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/9fc48a02f3e79af371d9c78b1fa255164c72a042)
- JPA 추가
- AiChatRoom, AiChatRoomMessage 엔티티 추가
- GET /ai/chat 에 접속하면 채팅방 생성

# 5강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/25edffe5492f43667657d2bca14563a8b12123bd)
- gitignore 추가 : db.mv.db, db.trace.db 
- JPA가 H2 파일DB를 사용하도록

# 6강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/9ea33c9eece787be203f1dae5e0f9ec65bd360fe)
- 매 채팅시 마다 사용자 질문과 LLM 답변을 묶어서 AiChatRoomMessage 테이블에 저장

# 7강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/f9de4687e72d58ea3cfb3c425bb15a1cf6d30a09)
- GET /ai/chat/{roomId}/messages 를 통해서 해당 채팅방에서의 기존 메세지들 조회 API 구현

# 8강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/4290e1ebf40ded1edee2db1b05342d2acd691f9b)
- 채팅방에 접속시 기존 채팅메세지들이 보여지도록

# 9강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/89e2be6f57e0a0c3684f66ba39a1fd0e306abb9d)
- LLM에 질문을 하기 전에 SystemMessage 와 PreviousMessages 를 10개 추가하여 대화의 맥락을 유지하도록

# 10강
- [커밋](https://github.com/jhs512/kp-2025-03-31--1/commit/main)
- 시스템 메세지를 더 자세히 작성
- 오래된 N개의 메세지가 아닌 가장 최근 N개의 메세지를 사용