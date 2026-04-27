package com.example.jhapcham.product;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @GetMapping
    public ResponseEntity<List<AttributeDTO>> getAll() {
        return ResponseEntity.ok(attributeService.getAllAttributes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeDTO> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(attributeService.getAttribute(id));
    }

    @PostMapping
    public ResponseEntity<AttributeDTO> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(attributeService.createAttribute(name));
    }

    @PostMapping("/{id}/values")
    public ResponseEntity<AttributeDTO> addValue(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(attributeService.addValue(id, value));
    }
}
