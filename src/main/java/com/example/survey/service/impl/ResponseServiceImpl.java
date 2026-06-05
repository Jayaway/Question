package com.example.survey.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.survey.dto.StatsDto;
import com.example.survey.entity.*;
import com.example.survey.mapper.*;
import com.example.survey.service.ResponseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResponseServiceImpl implements ResponseService {

    private final SurveyMapper surveyMapper;
    private final QuestionMapper questionMapper;
    private final ResponseMapper responseMapper;
    private final AnswerMapper answerMapper;

    public ResponseServiceImpl(SurveyMapper surveyMapper,
                               QuestionMapper questionMapper,
                               ResponseMapper responseMapper,
                               AnswerMapper answerMapper) {
        this.surveyMapper = surveyMapper;
        this.questionMapper = questionMapper;
        this.responseMapper = responseMapper;
        this.answerMapper = answerMapper;
    }

    /**
     * 提交答卷。@Transactional 保证整个过程要么全部成功，要么全部回滚。
     *
     * 流程：
     *   1. 校验问卷存在且已发布
     *   2. 创建答卷记录（response）
     *   3. 逐条插入答案（answer）
     */
    @Override
    @Transactional  // ← 关键！保证数据一致性
    public void submit(Long surveyId, Map<Long, String> answers, String ip) {
        // 1. 校验问卷
        Survey survey = surveyMapper.selectById(surveyId);
        if (survey == null) {
            throw new IllegalArgumentException("问卷不存在");
        }
        if (survey.getStatus() != 1) {
            throw new IllegalArgumentException("问卷未发布，无法填写");
        }

        // 2. 创建答卷记录
        Response response = new Response();
        response.setSurveyId(surveyId);
        response.setRespondentIp(ip);
        responseMapper.insert(response);

        // 3. 逐条插入答案
        for (Map.Entry<Long, String> entry : answers.entrySet()) {
            Long questionId = entry.getKey();
            String content = entry.getValue();

            // 校验题目属于该问卷
            Question question = questionMapper.selectById(questionId);
            if (question == null || !question.getSurveyId().equals(surveyId)) {
                throw new IllegalArgumentException("题目 ID 不合法：" + questionId);
            }

            Answer answer = new Answer();
            answer.setResponseId(response.getId());
            answer.setQuestionId(questionId);
            answer.setContent(content);
            answerMapper.insert(answer);
        }
    }

    /**
     * 统计问卷数据。逐题统计：
     *   - 单选/多选题：统计每个选项被选了多少次
     *   - 文本题：列出所有回答
     *
     * 实现方式：查出所有答案，在 Java 中分组计数。
     * 如果数据量大（> 10 万条），应该用 SQL GROUP BY。
     */
    @Override
    public List<StatsDto> stats(Long surveyId) {
        // 1. 查出问卷的所有题目
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, surveyId)
                        .orderByAsc(Question::getSortOrder));

        // 2. 查出该问卷的所有答卷 ID
        List<Response> responses = responseMapper.selectList(
                new LambdaQueryWrapper<Response>()
                        .eq(Response::getSurveyId, surveyId));

        if (responses.isEmpty()) {
            return questions.stream().map(q -> {
                StatsDto dto = new StatsDto();
                dto.setQuestionId(q.getId());
                dto.setQuestionTitle(q.getTitle());
                dto.setType(q.getType());
                dto.setOptions(new ArrayList<>());
                dto.setTexts(new ArrayList<>());
                return dto;
            }).collect(Collectors.toList());
        }

        Set<Long> responseIds = responses.stream()
                .map(Response::getId).collect(Collectors.toSet());

        // 3. 查出所有答案
        List<Answer> allAnswers = answerMapper.selectList(
                new LambdaQueryWrapper<Answer>()
                        .in(Answer::getResponseId, responseIds));

        // 4. 按 questionId 分组
        Map<Long, List<Answer>> answersByQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(Answer::getQuestionId));

        // 5. 逐题构造统计结果
        List<StatsDto> result = new ArrayList<>();
        for (Question q : questions) {
            StatsDto dto = new StatsDto();
            dto.setQuestionId(q.getId());
            dto.setQuestionTitle(q.getTitle());
            dto.setType(q.getType());

            List<Answer> answers = answersByQuestion
                    .getOrDefault(q.getId(), Collections.emptyList());

            if (q.getType() == 3) {
                // 文本题：收集所有回答文本
                dto.setTexts(answers.stream()
                        .map(Answer::getContent)
                        .collect(Collectors.toList()));
                dto.setOptions(new ArrayList<>());
            } else {
                // 选择题：统计每个选项被选中的次数
                // 对于多选，用户答案可能是 "A,B"，需要拆分
                Map<String, Long> countMap = new HashMap<>();
                for (String opt : parseOptions(q.getOptions())) {
                    countMap.put(opt, 0L);
                }
                for (Answer a : answers) {
                    String[] selected = a.getContent().split(",");
                    for (String s : selected) {
                        String trimmed = s.trim();
                        countMap.merge(trimmed, 1L, Long::sum);
                    }
                }
                dto.setOptions(countMap.entrySet().stream()
                        .map(e -> new StatsDto.OptionCount(e.getKey(), e.getValue()))
                        .collect(Collectors.toList()));
                dto.setTexts(new ArrayList<>());
            }
            result.add(dto);
        }
        return result;
    }

    /** 解析 options JSON 字符串为 List<String> */
    private List<String> parseOptions(String options) {
        if (options == null || options.isBlank()) return Collections.emptyList();
        // 格式：["A","B","C"] → 去掉首尾 [] → 按逗号拆分 → 去引号
        String trimmed = options.substring(1, options.length() - 1);
        if (trimmed.isBlank()) return Collections.emptyList();
        return Arrays.stream(trimmed.split(","))
                .map(s -> s.replace("\"", "").trim())
                .collect(Collectors.toList());
    }
}
