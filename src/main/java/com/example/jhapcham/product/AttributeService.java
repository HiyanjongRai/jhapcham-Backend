package com.example.jhapcham.product;

import com.example.jhapcham.Error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    // ── Attributes ──────────────────────────────────────────────

    public List<AttributeDTO> getAllAttributes() {
        return attributeRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AttributeDTO getAttribute(Long id) {
        return toDTO(attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found")));
    }

    @Transactional
    public AttributeDTO createAttribute(String name) {
        if (attributeRepository.existsByNameIgnoreCase(name)) {
            return toDTO(attributeRepository.findByNameIgnoreCase(name).get());
        }
        Attribute attr = Attribute.builder().name(name).build();
        return toDTO(attributeRepository.save(attr));
    }

    /**
     * Find or create an attribute by name (case-insensitive).
     * Used during product/variant creation.
     */
    @Transactional
    public Attribute findOrCreateAttribute(String name) {
        return attributeRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> attributeRepository.save(Attribute.builder().name(name).build()));
    }

    // ── Attribute Values ─────────────────────────────────────────

    @Transactional
    public AttributeDTO addValue(Long attributeId, String value) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found"));

        attributeValueRepository.findByAttributeAndValueIgnoreCase(attribute, value)
                .ifPresent(v -> { throw new RuntimeException("Value '" + value + "' already exists for attribute '" + attribute.getName() + "'"); });

        AttributeValue av = AttributeValue.builder()
                .attribute(attribute)
                .value(value)
                .build();
        attributeValueRepository.save(av);
        log.info("Added value '{}' to attribute '{}'", value, attribute.getName());
        return toDTO(attribute);
    }

    /**
     * Find or create an AttributeValue by attribute name + value string.
     * Used during variant creation from seller product forms.
     */
    @Transactional
    public AttributeValue findOrCreateValue(String attributeName, String value) {
        Attribute attribute = findOrCreateAttribute(attributeName);
        return attributeValueRepository.findByAttributeAndValueIgnoreCase(attribute, value)
                .orElseGet(() -> attributeValueRepository.save(
                        AttributeValue.builder().attribute(attribute).value(value).build()
                ));
    }

    public AttributeValue getAttributeValue(Long id) {
        return attributeValueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeValue not found: " + id));
    }

    // ── Mapping ──────────────────────────────────────────────────

    private AttributeDTO toDTO(Attribute attr) {
        List<AttributeDTO.AttributeValueDTO> values = attributeValueRepository.findByAttribute(attr)
                .stream()
                .map(av -> AttributeDTO.AttributeValueDTO.builder()
                        .id(av.getId())
                        .value(av.getValue())
                        .build())
                .collect(Collectors.toList());

        return AttributeDTO.builder()
                .id(attr.getId())
                .name(attr.getName())
                .values(values)
                .build();
    }
}
