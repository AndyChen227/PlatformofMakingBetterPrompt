package com.betterprompt.betterpromptbyandyy2.generator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the Prompt Generator feature.
 *
 * Endpoints:
 *   GET /api/generator/prompt?type=CODING&verbosity=HIGH&source=template
 *
 * Query parameters:
 *   type      — task type (case-insensitive): CODING | EXPLAIN | DEBUG | WRITING | COMPARE
 *   verbosity — verbosity level (case-insensitive): LOW | MEDIUM | HIGH
 *   source    — generator backend (optional, default "template"): template | ai
 *
 * Response 200:
 *   { "prompt": "...", "taskType": "CODING", "verbosity": "HIGH", "source": "template" }
 *
 * Response 400:
 *   { "error": "..." }
 */
@RestController
@RequestMapping("/api/generator")
public class PromptGeneratorController {

    private final TemplatePromptGenerator templateGenerator;
    private final AiPromptGenerator       aiGenerator;

    public PromptGeneratorController(TemplatePromptGenerator templateGenerator,
                                     AiPromptGenerator aiGenerator) {
        this.templateGenerator = templateGenerator;
        this.aiGenerator       = aiGenerator;
    }

    @GetMapping("/prompt")
    public ResponseEntity<?> generatePrompt(
            @RequestParam("type")                        String typeRaw,
            @RequestParam("verbosity")                   String verbosityRaw,
            @RequestParam(value = "source", defaultValue = "template") String sourceRaw) {

        // Normalise to upper-case
        String taskType  = typeRaw.toUpperCase().trim();
        String verbosity = verbosityRaw.toUpperCase().trim();
        String source    = sourceRaw.toLowerCase().trim();

        // Validate task type
        if (!TemplatePromptGenerator.VALID_TASK_TYPES.contains(taskType)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid type '" + typeRaw + "'. Valid values: " +
                         String.join(", ", TemplatePromptGenerator.VALID_TASK_TYPES)
            ));
        }

        // Validate verbosity
        if (!TemplatePromptGenerator.VALID_VERBOSITY_LEVELS.contains(verbosity)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid verbosity '" + verbosityRaw + "'. Valid values: " +
                         String.join(", ", TemplatePromptGenerator.VALID_VERBOSITY_LEVELS)
            ));
        }

        // Validate source
        if (!source.equals("template") && !source.equals("ai")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid source '" + sourceRaw + "'. Valid values: template, ai"
            ));
        }

        // Generate prompt
        String prompt;
        if ("ai".equals(source)) {
            prompt = aiGenerator.generate(taskType, verbosity);
        } else {
            prompt = templateGenerator.generate(taskType, verbosity);
            if (prompt == null) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "error", "No template found for " + taskType + "/" + verbosity
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
            "prompt",    prompt,
            "taskType",  taskType,
            "verbosity", verbosity,
            "source",    source
        ));
    }
}
