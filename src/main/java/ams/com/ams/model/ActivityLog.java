package ams.com.ams.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content")
    private String content;

    @Column(name = "entity")
    private String entity;

    @Column(name = "timestamp")
    private Date timestamp;

    @Column(name = "username")
    private String username;
    public ActivityLog() {
    }

    public ActivityLog(String content, String entity, Date timestamp, String username) {
        this.content = content;
        this.entity = entity;
        this.timestamp = timestamp;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}