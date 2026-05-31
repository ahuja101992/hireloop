package com.hireloop.provider;

public class Prompts {
    public static String scoreJobPrompt(String resume, String jobDescription) {
        return String.format("""
            You are a job fit scoring expert. Analyze the candidate's resume against the job description.

            Resume:
            %s

            Job Description:
            %s

            Respond with ONLY a JSON object in a code block (no other text). Return exactly this format:
            ```json
            {
              "fit_score": <0-100 number>,
              "key_matches": [<list of matching skills/experiences>],
              "gaps": [<list of missing skills/experiences>],
              "recommendation": "<brief recommendation>"
            }
            ```
            """, resume, jobDescription);
    }

    public static String adaptResumePrompt(String resume, String jobDescription) {
        return String.format("""
            You are a resume tailoring expert. Tailor the candidate's resume for this specific job.

            Original Resume:
            %s

            Target Job:
            %s

            Respond with ONLY a JSON object in a code block (no other text). Return exactly this format:
            ```json
            {
              "tailored_summary": "<adapted professional summary>",
              "highlighted_skills": [<list of skills to highlight>],
              "relevant_experiences": [<list of relevant work experiences with keyword alignment>],
              "recommended_additions": [<suggested projects or skills to mention>]
            }
            ```
            """, resume, jobDescription);
    }

    public static String extractIntelPrompt(String rawText) {
        return String.format("""
            You are an interview intelligence analyst. Extract structured interview process information for Principal/Staff SWE interviews.

            Raw Data:
            %s

            Respond with ONLY a JSON object in a code block (no other text). Return exactly this format:
            ```json
            {
              "rounds": [
                {
                  "name": "<round name>",
                  "format": "<phone|video|onsite>",
                  "topics": ["<topic1>", "<topic2>"],
                  "difficulty": "<easy|medium|hard>"
                }
              ],
              "topic_frequencies": [
                {
                  "topic": "<topic name>",
                  "category": "DSA|SYSTEM_DESIGN|BEHAVIORAL",
                  "frequency": <0.0-1.0>
                }
              ]
            }
            ```
            """, rawText);
    }

    public static String generateBriefPrompt(String companyName, String intelJson) {
        return String.format("""
            You are a company interview brief generator. Generate interview prep guidance for a Principal/Staff SWE candidate.

            Company: %s
            Interview Intel: %s

            Respond with ONLY a JSON object in a code block (no other text). Return exactly this format:
            ```json
            {
              "rounds": ["<round description>"],
              "interview_tips": ["<tip 1>", "<tip 2>"],
              "suggested_lc_problems": ["<problem name or link>"],
              "system_design_cases": ["<case name>"],
              "behavioral_focus": ["<focus area>"]
            }
            ```
            """, companyName, intelJson);
    }
}
