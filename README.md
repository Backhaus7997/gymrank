# 🏋️ GymRank Argentina

Una aplicación Android nativa para competencias de gimnasios en Argentina, construida con Jetpack Compose y arquitectura MVVM.

## 🎯 Descripción

GymRank Argentina es una plataforma multi-tenant donde cada gimnasio tiene su propia app branded. Los usuarios compiten a través de rankings y ligas.

**Estado actual**: Implementación de Login (v0.1)

## 🚀 Tecnologías

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Navegación**: Compose Navigation
- **State Management**: StateFlow
- **Async**: Coroutines
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## 📦 Estructura del Proyecto

```
app/src/main/java/com/example/gymrank/
├── MainActivity.kt
├── navigation/          # Navegación de la app
├── domain/             # Modelos y contratos de repositorio
├── data/               # Implementaciones de repositorio (mock)
└── ui/
    ├── screens/        # Pantallas (Login, Home)
    └── theme/          # Colores, tipografía, tema
```

## ✨ Funcionalidades Implementadas

### ✅ Login
- Email + Password authentication (mock)
- Validación de formato de email
- Loading states
- Error handling
- Navegación post-login

## 🏃 Cómo Ejecutar

### Android Studio
1. Clonar/abrir el proyecto
2. Sincronizar Gradle
3. Ejecutar en emulador o dispositivo

### Línea de comandos
```bash
./gradlew build
./gradlew installDebug
```

## 🧪 Credenciales de Prueba

Como usa autenticación mock:
- **Email**: cualquier email válido (ej: `test@test.com`)
- **Password**: cualquier texto no vacío

## 📱 Screenshots

_Login Screen_
- Input de email con validación
- Input de password
- Botón de login con loading state
- Mensajes de error inline

## 🔮 Roadmap

- [ ] Sistema de Rankings
- [ ] Ligas competitivas
- [ ] Perfiles de usuario
- [ ] Multi-tenant (apps por gimnasio)
- [ ] Backend real (REST/GraphQL)
- [ ] Persistencia local (Room)
- [ ] Push notifications

## 👨‍💻 Desarrollo

Este proyecto sigue Clean Architecture y principios SOLID:
- Separación de capas (UI, Domain, Data)
- Repository pattern
- Dependency injection preparado
- Testeable

## 📄 Licencia

Proyecto de desarrollo privado

---

**Versión**: 0.1.0  
**Fecha**: 2026-02-01
