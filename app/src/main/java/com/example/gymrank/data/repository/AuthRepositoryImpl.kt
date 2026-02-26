package com.example.gymrank.data.repository

import com.example.gymrank.domain.model.User
import com.example.gymrank.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fu = result.user ?: return Result.failure(IllegalStateException("Usuario no disponible"))

            // ✅ Opcional: actualizar updatedAt al loguear
            runCatching {
                db.collection("users")
                    .document(fu.uid)
                    .set(
                        mapOf("updatedAt" to FieldValue.serverTimestamp()),
                        SetOptions.merge()
                    )
                    .await()
            }

            Result.success(
                User(
                    id = fu.uid,
                    email = fu.email ?: email.trim(),
                    name = fu.displayName ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(firebaseAuthMessage(e)))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val fu = result.user ?: return Result.failure(IllegalStateException("Usuario no disponible"))

            val user = User(
                id = fu.uid,
                email = fu.email ?: email.trim(),
                name = fu.displayName ?: ""
            )

            // ✅ Guardar en Firestore (timestamps como "fecha")
            val userDoc = mapOf(
                "id" to user.id,
                "uid" to user.id, // opcional, pero útil
                "email" to user.email,
                "name" to user.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("users")
                .document(user.id)
                .set(userDoc, SetOptions.merge())
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(firebaseAuthMessage(e)))
        }
    }

    // ✅ Google signup/login con idToken
    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val fu = result.user ?: return Result.failure(IllegalStateException("Usuario no disponible"))

            val user = User(
                id = fu.uid,
                email = fu.email ?: "",
                name = fu.displayName ?: ""
            )

            val userRef = db.collection("users").document(user.id)

            // ✅ createdAt solo si no existe
            val snap = userRef.get().await()

            val data = mutableMapOf<String, Any?>(
                "id" to user.id,
                "uid" to user.id,
                "email" to user.email,
                "name" to user.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!snap.exists()) {
                data["createdAt"] = FieldValue.serverTimestamp()
            }

            userRef.set(data, SetOptions.merge()).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(firebaseAuthMessage(e)))
        }
    }

    // ✅ NUEVO: enviar email de recuperación de contraseña
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(firebaseAuthMessage(e)))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun firebaseAuthMessage(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode
            ?: return (e.message ?: "Error de autenticación")

        return when (code) {
            "ERROR_INVALID_EMAIL" -> "El correo es inválido."
            "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con ese correo."
            "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta."
            "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Probá de nuevo más tarde."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Revisá tu internet."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Ese correo ya está registrado."
            "ERROR_WEAK_PASSWORD" -> "Contraseña débil. Usá 6+ caracteres."
            else -> e.message ?: "Error de autenticación"
        }
    }
}