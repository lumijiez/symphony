package io.github.lumijiez.data.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "push_data")
public class PushData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nodeAddress;

    @Column(nullable = false)
    private String data;

    public PushData() {
    }

    public PushData(String nodeAddress, String data) {
        this.nodeAddress = nodeAddress;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

