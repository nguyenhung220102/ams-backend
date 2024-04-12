package ams.com.ams.model;

import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "available_quantity")
    private int availableQuantity;

    @Column(name = "damaged_quantity")
    private int damagedQuantity;

    @Column(name = "missing_quantity")
    private int missingQuantity;

    @ManyToMany(mappedBy = "assets")
    @JsonBackReference
    @JsonIgnoreProperties("departments")
    private List<Department> departments;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Asset() {
    }

    public Asset(String name, int quantity, Category category) {
        this.name = name;
        this.quantity = quantity;
        this.availableQuantity = quantity;
        this.damagedQuantity = 0;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getDamagedQuantity() {
        return damagedQuantity;
    }

    public void setDamagedQuantity(int damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }

    public int getMissingQuantity() {
        return missingQuantity;
    }

    public void setMissingQuantity(int missingQuantity) {
        this.missingQuantity = missingQuantity;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public Category getCategory(){
        return category;
    }

    public void setCategory(Category category){
        this.category = category;
    }
}