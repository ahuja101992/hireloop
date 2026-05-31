package com.hireloop.provider;

public class Prompts {
    public static String scoreJobPrompt(String resume, String jobDescription) {
        return String.format("""
            You are a job fit scoring expert. Analyze the candidate's resume against the job description.

            Resume:
            %s

            Job Description:
            %s

            Respond with a JSON object containing:
            {
              "fit_score": <0-100 number>,
              "key_matches": [<list of matching skills/experiences>],
              "gaps": [<list of missing skills/experiences>],
              "recommendation": "<brief recommendation>"
            }
            """, resume, jobDescription);
    }

    public static String adaptResumePrompt(String resume, String jobDescription) {
        return String.format("""
            You are a resume tailoring expert. Tailor the candidate's resume for this specific job.

            Original Resume:
            %s

            Target Job:
            %s

            Respond with a JSON object containing:
            {
              "tailored_summary": "<adapted professional summary>",
              "highlighted_skills": [<list of skills to highlight>],
              "relevant_experiences": [<list of relevant work experiences with keyword alignment>],
              "recommended_additions": [<suggested projects or skills to mention>]
            }
            """, resume, jobDescription);
    }

    public static String extractIntelPrompt(String rawText) {
        return String.format("""
            You are an interview intelligence analyst. Extract structured interview process information.

            Raw Data:
            %s

            Respond with a JSON object containing:
            {
              "rounds": [
                {
                  "round_number": <number>,
                  "name": "<round name>",
                  "duration_minutes": <number>,
                  "topics": [<topics covered>],
                  "difficulty": "<easy|medium|hard>"
                }
              ],
              "key_topics": [<list of important topics>],
              "estimated_total_duration": "<estimated total interview duration>"
            }
            """, rawText);
    }

    public static String generateBriefPrompt(String companyName, String topicsJson) {
        return String.format("""
            You are a company brief generator. Create a concise interview preparation brief.

            Company: %s
            Topics to Cover: %s

            Respond with a JSON object containing:
            {
              "company_overview": "<brief company background>",
              "interview_focus": [<key topics they care about>],
              "preparation_tips": [<specific tips for this company>],
              "common_questions": [<typical questions asked>]
            }
            """, companyName, topicsJson);
    }
}
