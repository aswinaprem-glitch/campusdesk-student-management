# CampusDesk — Student Management System (Java OOP Lab)

A small local web application with an HTML/CSS/JavaScript interface and a Java backend using the built-in `HttpServer`. No external Java libraries are required.

## Requirements
- JDK 17 or newer (JDK 11+ may also work)
- A modern web browser

## Run it
1. Extract the ZIP file.
2. Open a terminal in the `StudentManagementOOP` folder (the folder containing `Main.java`).
3. Compile:
   ```bash
   javac Main.java
   ```
4. Start the server:
   ```bash
   java Main
   ```
5. Open `http://localhost:8080` in your browser.

Keep the terminal running while using the application. Press `Ctrl+C` in the terminal to stop it.

## Demo login
- User ID: `admin`
- Password: `admin123`

These are demonstration credentials for a college lab project, not secure production authentication.

## Six student operations
1. **Add** — create a student record.
2. **View** — view a student's details (the search result is the view).
3. **Update** — edit an existing record.
4. **Delete** — remove a record.
5. **Search** — find a student by ID.
6. **Display all** — show the complete collection of records (final display).

The application displays a result message after actions. The overview and All Records screen display the current collection.

## OOP concepts to explain in viva
- **Class:** `Student`, `StudentManager`.
- **Object:** each student record is a `Student` object.
- **Encapsulation:** student fields are private and accessed through getters/setters.
- **Methods:** `addStudent`, `findStudent`, `deleteStudent`, `getAllStudents`.
- **Separation of responsibility:** `Student` represents data, `StudentManager` manages records, `Main` handles HTTP requests, and the public folder contains the interface.

## Important limitations
- Records are stored in memory and reset when the Java server restarts.
- This is a lab demonstration, not a production-ready login system.
- The project uses a simple local server and does not require a database unless your lecturer separately asks for one.

## Suggested demonstration order
1. Login with the demo account.
2. Display all students.
3. Add a student with a new ID.
4. Search for that ID.
5. View the student's details.
6. Update the student's course or email.
7. Delete the record.
8. Open All Records and show the final collection.
