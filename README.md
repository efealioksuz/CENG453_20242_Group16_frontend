# CENG453_20242_Group16_frontend

Frontend repository for the **CENG453 Term Project**, developed by a team of two students – a JavaFX-based UNO game.

This project supports both **single-player** and **multiplayer** modes.

The frontend is built using **JavaFX**, and both the frontend and backend were developed collaboratively with my teammate. The frontend connects to a separately hosted backend.

---

## 🔗 Backend Repository

The backend for this project is hosted on Render and source code can be accessed via the following repository:

👉 [CENG453_20242_Group16_backend](https://github.com/dgukan35/CENG453_20242_Group16_backend)

Make sure the backend is running before launching the frontend.

> ⚠️ **Note:**  
> Since the backend is hosted on Render, it may enter sleep mode after a period of inactivity. Before running the frontend, **manually visit the backend URL in your browser** to wake it up. This ensures a smooth multiplayer experience.  Backend Render URL: https://ceng453-20242-group16-backend.onrender.com/swagger-ui/index.html

---

## 🚀 How to Run the Frontend

### Option 1: Run from IntelliJ IDEA

1. Open the project in IntelliJ IDEA.  
2. Make sure your JDK version is compatible with JavaFX and Spring Boot.  
3. Locate the `SpringbootApplication.java` file in the `src/main/java/...` directory.  
4. Right-click on the file and select **Run 'SpringbootApplication'**.  

> This will start the JavaFX-based UNO game.

---

### Option 2: Build via Terminal

```bash
./gradlew build

This command builds the project. The exact command to run the application may vary depending on your environment and build configuration.

