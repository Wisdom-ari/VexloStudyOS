package com.vexlo.studyos.data

/** Default alarm schedule requested for Vexlo StudyOS. Mon-Sat = 63, every day = 127. */
suspend fun seedAlarms(dao: StudyDao) {
    val monSat = 63
    val all = 127
    listOf(
        AlarmEntity(hour = 5, minute = 0, label = "Wake up", type = "WAKE", daysMask = monSat),
        AlarmEntity(hour = 5, minute = 30, label = "Study session 1", type = "STUDY", daysMask = monSat),
        AlarmEntity(hour = 7, minute = 30, label = "Break", type = "BREAK", daysMask = monSat),
        AlarmEntity(hour = 8, minute = 0, label = "Study session 2", type = "STUDY", daysMask = monSat),
        AlarmEntity(hour = 10, minute = 0, label = "Break", type = "BREAK", daysMask = monSat),
        AlarmEntity(hour = 10, minute = 30, label = "Study session 3", type = "STUDY", daysMask = monSat),
        AlarmEntity(hour = 12, minute = 30, label = "Lunch", type = "LUNCH", daysMask = monSat),
        AlarmEntity(hour = 14, minute = 0, label = "Study session 4", type = "STUDY", daysMask = monSat),
        AlarmEntity(hour = 16, minute = 0, label = "End of focused study", type = "END", daysMask = monSat),
        AlarmEntity(hour = 19, minute = 0, label = "Optional light revision", type = "REVISION", daysMask = all)
    ).forEach { dao.insertAlarm(it) }
}

/** First-run content. Starter topics are a generic outline - edit them to match your lecturer's syllabus. */
suspend fun seedIfEmpty(dao: StudyDao) {
    if (dao.courseCount() > 0) return

    val names = listOf(
        "Computer Architecture" to "CA",
        "Fundamentals of Database Design" to "DB",
        "Fundamentals of User Interface Design" to "UI",
        "Research Methods" to "RM",
        "Visual Programming (C# Windows Forms)" to "VP",
        "Gender and ICT" to "GICT"
    )
    val ids = names.mapIndexed { i, (n, s) ->
        dao.insertCourse(CourseEntity(name = n, shortName = s, colorIdx = i))
    }
    val (ca, db, ui, rm, vp, gi) = ids

    val outline = mapOf(
        ca to listOf("Number systems and data representation", "CPU organisation and registers", "Instruction set architecture", "Memory hierarchy and cache", "Input/output and buses", "Pipelining and performance"),
        db to listOf("Database concepts and DBMS", "ER modelling", "Relational model and keys", "Normalisation", "SQL: DDL and DML", "Joins and queries", "Transactions and indexing"),
        ui to listOf("HCI principles", "Usability heuristics", "Wireframing and prototyping", "Colour, typography and layout", "Interaction design", "Usability evaluation"),
        rm to listOf("Research problem and objectives", "Literature review", "Research design", "Sampling techniques", "Data collection methods", "Ethics and referencing"),
        vp to listOf("Windows Forms controls", "Events and handlers", "Menus and dialogs", "ADO.NET database connectivity", "Collections and LINQ", "Exceptions and debugging"),
        gi to listOf("Gender concepts", "Digital divide", "Women in ICT", "Gender and ICT policy", "Online safety and gender-based violence", "Gender mainstreaming in ICT projects")
    )
    outline.forEach { (cid, list) -> list.forEach { dao.insertTopic(TopicEntity(courseId = cid, title = it)) } }

    val checks = listOf("Review all lecture notes", "Attempt past paper questions", "Revise weak topics", "Summarise key definitions")
    ids.forEach { cid -> checks.forEach { dao.insertChecklist(ChecklistEntity(courseId = cid, text = it)) } }

    // Weekly timetable (1 = Monday ... 7 = Sunday)
    val rows = listOf(
        Triple(1, ca, "Computer Architecture"), Triple(1, db, "Database Design"),
        Triple(2, vp, "Visual Programming (C#)"), Triple(2, ui, "User Interface Design"),
        Triple(3, ca, "Computer Architecture"), Triple(3, rm, "Research Methods"),
        Triple(4, db, "Database Design"), Triple(4, vp, "Visual Programming"),
        Triple(5, ui, "User Interface Design"), Triple(5, gi, "Gender and ICT"),
        Triple(6, ca, "Computer Architecture")
    )
    var order = 0
    rows.forEach { (day, cid, title) -> dao.insertTimetable(TimetableEntity(dayOfWeek = day, courseId = cid, title = title, sortOrder = order++)) }
    dao.insertTimetable(TimetableEntity(dayOfWeek = 6, courseId = null, title = "Weekly examination practice", sortOrder = order++))
    dao.insertTimetable(TimetableEntity(dayOfWeek = 6, courseId = null, title = "Revision of all courses", sortOrder = order++))
    dao.insertTimetable(TimetableEntity(dayOfWeek = 7, courseId = null, title = "Rest and optional light revision", sortOrder = order++))

    seedAlarms(dao)
}

private operator fun <T> List<T>.component6(): T = this[5]
