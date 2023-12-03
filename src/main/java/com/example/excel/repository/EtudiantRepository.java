package com.example.excel.repository;


import com.example.excel.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant,String> {
//    Etudiant findByCNE(String cne);
}
