package org.uu.nl.embedding.bca.util;

import grph.properties.Property;
import org.uu.nl.embedding.bca.util.BCV;
import org.uu.nl.embedding.util.InMemoryRdfGraph;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BCVAdapter {

    private static final String PERSON_URI = "/person";

    public BCV adapt(BCV bcv, final InMemoryRdfGraph graph,
    		final int[][] inVertex, final int[][] outVertex,
    		final int[][] inEdge, final int[][] outEdge) {

        final int bookmark = bcv.getRootNode();
        final Property vertexLabels = graph.getVertexLabelProperty();

        // Leave non person nodes unchanged
        if(!vertexLabels.getValueAsString(bookmark).contains(PERSON_URI)) return bcv;

        final Person p1 = new Person(bookmark, graph, inVertex, outVertex, inEdge, outEdge);

        final Rule[] rules = { new BirthDateBeforeDeathDateRule() }; 
        // TODO: get rules from config or something

        for(int vert : bcv.keySet()) {

            // Skip other non person nodes
            if(!vertexLabels.getValueAsString(vert).contains(PERSON_URI)) continue;

            Person p2 = new Person(vert, graph, inVertex, outVertex, inEdge, outEdge);

            if(isRuleConflict(p1, p2, rules)) {
                bcv.remove(vert);
            }
        }
        
        return bcv;
    }

    private boolean isRuleConflict(Person p1, Person p2, Rule[] rules) {
        for(Rule rule : rules) {
            if(rule.isConflict(p1, p2)) return true;
        }
        return false;
    }

    interface Rule {
         boolean isConflict(Person p1, Person p2);
    }

    class BirthDateBeforeDeathDateRule implements Rule {
        public boolean isConflict(Person p1, Person p2) {
            return p1.birthDate.isAfter(p2.deathDate);
        }
    }

    class Person {
        String name;
        LocalDate birthDate;
        LocalDate deathDate;
        LocalDate baptismDate;
        //etc...

        public Person(int vert, InMemoryRdfGraph graph, int[][] inVertex, int[][] outVertex, int[][] inEdge, int[][] outEdge) {
            // construct values from graph here
            
            for(int outVert : outVertex[vert]) {
                int edge = outEdge[vert][outVert];
                
                String predicate = graph.getEdgeLabelProperty().getValueAsString(edge).toLowerCase();
                String value = graph.getVertexLabelProperty().getValueAsString(outVert);
                
                if(predicate.contains("name")) {
                    this.name = value;
                } else if (predicate.contains("birthdate")) {
                    // Check org.uu.nl.embedding.util.similarity.Date.java to see how to get a specific formatter for a pattern (e.g. yyyy-MM-dd)
                    this.birthDate = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                } else if (predicate.contains("deatdate")) {
                    // etc...
                }
            }
            
        }
    }
}