package org.example.face;

import org.example.model.Message;
import org.example.opencv.OpenCVImage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Face {

    private final OpenCVImage imageFace;
    private final int id;
    private Person person;
    private LocalDateTime meetingTime;
    private List<Message> messages = new ArrayList<>();

    public Face(OpenCVImage imageFace, int id) {
        this.imageFace = imageFace;
        this.id = id;
    }

    public OpenCVImage getImageFace() {
        return imageFace;
    }

    public int getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person, LocalDateTime meetingTime) {
        this.person = person;
        this.meetingTime = meetingTime;
    }

    public LocalDateTime getMeetingTime() {
        return meetingTime;
    }

    public void setMeetingTime(LocalDateTime meetingTime) {
        this.meetingTime = meetingTime;
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "Face{" +
                "person=" + person +
                ", id=" + id +
                '}';
    }
}
