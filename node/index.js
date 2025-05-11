const express = require("express");
const PORT = process.env.PORT || "3000";
const mysql = require("mysql2");
const bodyParser = require("body-parser");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(bodyParser.json());

const db = mysql.createConnection({
  host: "localhost",
  port: 3309,
  user: "root",
  password: "123A",
  database: "tracker_db",
});

app.get('/activity', (req, res) => {
    db.query('SELECT * FROM user_activity', (err, results) => {
        if (err) return res.status(500).send(err);
        res.json(results);
    });
});

app.post('/activity', (req, res) => {
    const { user_group, activity_date, steps, distance_km, active_time_minutes } = req.body;
    const sql = 'INSERT INTO user_activity (user_group, activity_date, steps, distance_km, active_time_minutes) VALUES (?, ?, ?, ?, ?)';
    db.query(sql, [user_group, activity_date, steps, distance_km, active_time_minutes], (err, result) => {
        if (err) return res.status(500).send(err);
        res.json({ id: result.insertId, ...req.body });
    });
});
app.listen(PORT, () => console.log("API started on port 3000"));
