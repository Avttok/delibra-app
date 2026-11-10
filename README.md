# Delibra · app Android

App da Delibra para motoboys, supervisores e gestores (Kotlin + Jetpack Compose).

- O APK sai sozinho a cada envio para a `main` (Actions → *Build APK* → Release `build-N`).
- Banco, site e repositório ficam em `gradle.properties` (só valores públicos).
- **Avisos push (Firebase):** no console do Firebase, adicione um app Android com o pacote
  `com.delibra.app`, baixe o `google-services.json` e troque o de `app/`. Sem isso o app
  funciona normalmente, só sem os avisos push com o app fechado.
