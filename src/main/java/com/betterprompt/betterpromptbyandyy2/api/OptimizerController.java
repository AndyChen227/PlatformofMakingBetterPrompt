package com.betterprompt.betterpromptbyandyy2.api;

import com.betterprompt.betterpromptbyandyy2.model.OptimizationRequest;
import com.betterprompt.betterpromptbyandyy2.model.OptimizationResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.RuleEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller exposing the optimization API.
 *
 * Endpoints:
 *   POST /api/optimize   — run the optimization pipeline
 *   GET  /api/rules      — list all registered rules and their metadata
 */
@RestController
@RequestMapping("/api")
public class OptimizerController {

    private final RuleEngine ruleEngine;
    private final List<Rule> rules;

    public OptimizerController(RuleEngine ruleEngine, List<Rule> rules) {
        this.ruleEngine = ruleEngine;
        this.rules = rules;
    }

    /**
     * Run the full optimization pipeline against the submitted prompt.
     *
     * @param request  JSON body with "prompt" and "rules" map
     * @return         400 if prompt is missing/blank, 200 with OptimizationResult otherwise
     */
    @PostMapping("/optimize")
    public ResponseEntity<?> optimize(@RequestBody OptimizationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "prompt must not be empty"));
        }
        OptimizationResult result = ruleEngine.optimize(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns metadata for all registered rules — used by the frontend to
     * render the rule configuration panel dynamically.
     */
    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, String>>> listRules() {
        List<Map<String, String>> metadata = rules.stream()
                .map(r -> Map.of(
                        "id",          r.getRuleId(),
                        "name",        r.getRuleName(),
                        "level",       r.getRuleLevel(),
                        "description", r.getDescription()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(metadata);
    }
}
