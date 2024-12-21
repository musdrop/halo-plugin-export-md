package cn.lyn4ever.export2md.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import cn.lyn4ever.export2md.halo.ArticalAttras;
import cn.lyn4ever.export2md.halo.Content;
import cn.lyn4ever.export2md.halo.ContentRequest;
import cn.lyn4ever.export2md.halo.ContentWrapper;
import cn.lyn4ever.export2md.halo.PostRequest;
import cn.lyn4ever.export2md.halo.service.AbstractContentService;
import cn.lyn4ever.export2md.service.PostService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import cn.lyn4ever.export2md.util.ArticalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.markdown4j.Markdown4jProcessor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;
import run.halo.app.infra.Condition;
import run.halo.app.infra.ConditionStatus;

/**
 * A default implementation of {@link PostService}.
 *
 * @author guqing，musdrop
 * @since 2.0.0
 */
@Slf4j
@Component
public class PostServiceImpl extends AbstractContentService implements PostService {
    private final ReactiveExtensionClient client;

    public PostServiceImpl(ReactiveExtensionClient client) {
        super(client);
        this.client = client;
    }


    /**
     * 保存文章
     *
     * @param postRequest
     * @return
     */
    @Override
    public Mono<Post> draftPost(PostRequest postRequest) {
        System.out.println(JSONUtil.toJsonStr(postRequest));
        return Mono.defer(
                () -> {
                    Post post = postRequest.getPost();
                    return getContextUsername()
                        .map(username -> {
                            post.getSpec().setOwner(username);
                            return post;
                        })
                        .defaultIfEmpty(post);
                }
            )
            //保存文章
            .flatMap(client::create)
            .flatMap(post -> {
                System.out.println("保存文章" + post.toString());
                if (postRequest.getContent() == null) {
                    return Mono.just(post);
                }
                var contentRequest =
                    new ContentRequest(Ref.of(post), post.getSpec().getHeadSnapshot(),
                        postRequest.getContent().getRaw(), postRequest.getContent().getContent(),
                        postRequest.getContent().getRawType());
                //保存文章内容
                return draftContent(post.getSpec().getBaseSnapshot(), contentRequest)
                    .flatMap(contentWrapper -> waitForPostToDraftConcludingWork(
                        post.getMetadata().getName(),
                        contentWrapper)
                    );
            })
            .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance));
    }

    private Mono<Post> waitForPostToDraftConcludingWork(String postName,
        ContentWrapper contentWrapper) {
        return Mono.defer(() -> client.fetch(Post.class, postName)
                .flatMap(post -> {
                    post.getSpec().setBaseSnapshot(contentWrapper.getSnapshotName());
                    post.getSpec().setHeadSnapshot(contentWrapper.getSnapshotName());
                    if (Objects.equals(true, post.getSpec().getPublish())) {
                        post.getSpec().setReleaseSnapshot(post.getSpec().getHeadSnapshot());
                    }
                    Condition condition = Condition.builder()
                        .type(Post.PostPhase.DRAFT.name())
                        .reason("DraftedSuccessfully")
                        .message("Drafted post successfully.")
                        .status(ConditionStatus.TRUE)
                        .lastTransitionTime(Instant.now())
                        .build();
                    Post.PostStatus status = post.getStatusOrDefault();
                    status.setPhase(Post.PostPhase.DRAFT.name());
                    status.getConditionsOrDefault().addAndEvictFIFO(condition);
                    return client.update(post);
                }))
            .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance));
    }

    @Override
    public Mono<Post> updatePost(PostRequest postRequest) {
        Post post = postRequest.getPost();
        String headSnapshot = post.getSpec().getHeadSnapshot();
        String releaseSnapshot = post.getSpec().getReleaseSnapshot();
        String baseSnapshot = post.getSpec().getBaseSnapshot();

        if (StringUtils.equals(releaseSnapshot, headSnapshot)) {
            // create new snapshot to update first
            return draftContent(baseSnapshot, postRequest.contentRequest(), headSnapshot)
                .flatMap(contentWrapper -> {
                    post.getSpec().setHeadSnapshot(contentWrapper.getSnapshotName());
                    return client.update(post);
                });
        }
        return Mono.defer(() -> updateContent(baseSnapshot, postRequest.contentRequest())
                .flatMap(contentWrapper -> {
                    post.getSpec().setHeadSnapshot(contentWrapper.getSnapshotName());
                    return client.update(post);
                }))
            .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                .filter(throwable -> throwable instanceof OptimisticLockingFailureException));
    }

    @Override
    public Mono<Post> updateBy(@NonNull Post post) {
        return client.update(post);
    }

    @Override
    public Mono<ContentWrapper> getHeadContent(String postName) {
        return client.get(Post.class, postName)
            .flatMap(this::getHeadContent);
    }

    @Override
    public Mono<ContentWrapper> getHeadContent(Post post) {
        var headSnapshot = post.getSpec().getHeadSnapshot();
        return getContent(headSnapshot, post.getSpec().getBaseSnapshot());
    }

    @Override
    public Mono<ContentWrapper> getReleaseContent(String postName) {
        return client.get(Post.class, postName)
            .flatMap(this::getReleaseContent);
    }

    @Override
    public Mono<ContentWrapper> getReleaseContent(Post post) {
        var releaseSnapshot = post.getSpec().getReleaseSnapshot();
        return getContent(releaseSnapshot, post.getSpec().getBaseSnapshot());
    }

    @Override
    public Mono<Post> publish(Post post) {
        return Mono.just(post)
            .doOnNext(p -> {
                var spec = post.getSpec();
                spec.setPublish(true);
                if (spec.getHeadSnapshot() == null) {
                    spec.setHeadSnapshot(spec.getBaseSnapshot());
                }
                spec.setReleaseSnapshot(spec.getHeadSnapshot());
            }).flatMap(client::update);
    }

    @Override
    public Mono<Post> unpublish(Post post) {
        return Mono.just(post)
            .doOnNext(p -> p.getSpec().setPublish(false))
            .flatMap(client::update);
    }

    @Override
    public Mono<Post> getByUsername(String postName, String username) {
        return client.get(Post.class, postName)
            .filter(post -> post.getSpec() != null)
            .filter(post -> Objects.equals(username, post.getSpec().getOwner()));
    }

    @Override
    public PostRequest formatPost(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //从文件中获取文章的属性，并将属性的内容从文章中移除
        ArticalAttras attrs = ArticalUtil.getAttributes(reader);

        //将文章的内容读取到一个StringJoiner中
        StringJoiner sj = new StringJoiner("\n");
        reader.lines().forEach(sj::add);

        //获取文章的标题
        String title = attrs.getTitle();
        if(title.equals("无标题")){
            //如果没有标题，就用文件名作为标题（去掉.md后缀）
            title = file.getName().substring(0,file.getName().length()-3);
        }
        //构造文章
        Post post = new Post();
        Post.PostSpec postSpec = new Post.PostSpec();
        postSpec.setTitle(title);//设置文章的标题
        postSpec.setSlug(UUID.fastUUID().toString(false));
        postSpec.setAllowComment(true);
        postSpec.setDeleted(false);
        Post.Excerpt excerpt = new Post.Excerpt();
        excerpt.setAutoGenerate(true);
        excerpt.setRaw("");
        postSpec.setExcerpt(excerpt);
        postSpec.setPriority(0);
        postSpec.setVisible(Post.VisibleEnum.PUBLIC);
        postSpec.setPublish(false);
        postSpec.setPinned(false);
        setAndCreateTags(attrs.getTags(),postSpec);//设置标签
        setAndCreateCategories(attrs.getCategories(),postSpec);//设置分类
        //设置文章的创建时间
        postSpec.setPublishTime(attrs.getDate().toInstant());


        Post.PostStatus postStatus = new Post.PostStatus();
        //草稿箱，待发布状态
        postStatus.setPhase(Post.PostPhase.DRAFT.name());
        // postStatus.setContributors(List.of(owner));

        post.setSpec(postSpec);
        // post.setStatus(postStatus);
        //设置元数据才能保存
        Metadata postMeta = new Metadata();
        postMeta.setName(UUID.fastUUID().toString(false));
        // postMeta.setAnnotations(Map.of("content.halo.run/preferred-editor","bytemd"));
        post.setMetadata(postMeta);


        try {
            return new PostRequest(post, new Content(sj.toString(),
                new Markdown4jProcessor().process(sj.toString()),
                "markdown")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    //根据标签的名称为文章添加标签，如果无该标签则创建标签
    private void setAndCreateTags(List<String> tags, Post.PostSpec postSpec) {
        client.list(Tag.class, null, null)
            .collectList()
            .subscribe(tagList -> {
                String meta_name ="";
                List<String> mns = new java.util.ArrayList<>(List.of());
                for (String tag : tags) {
                    //标签名是否已存在
                    boolean isExist = false;
                    for (Tag t : tagList) {
                        if (t.getSpec().getDisplayName().equals(tag)) {
                            isExist = true;
                            //存在，获取元数据名
                            meta_name = t.getMetadata().getName();
                            break;
                        }
                    }
                    //不存在则创建
                    if (!isExist) {
                        Tag newTag = new Tag();
                        Tag.TagSpec spec = new Tag.TagSpec();
                        Metadata metadata = new Metadata();
                        //设置标签的属性
                        spec.setDisplayName(tag);
                        spec.setSlug(UUID.fastUUID().toString(false));
                        spec.setColor("#ffffff");
                        spec.setCover(null);
                        //设置元数据
                        meta_name = UUID.fastUUID().toString(false);
                        metadata.setName(meta_name);
                        newTag.setSpec(spec);
                        newTag.setMetadata(metadata);
                        //创建新的标签
                        Mono.defer(() -> client.create(newTag)).subscribe();
                    }
                    mns.add(meta_name);
                }
                postSpec.setTags(mns);
            });
    }
    //根据标签的名称为文章添加分类，如果无该分类则创建分类
    private void setAndCreateCategories(List<String> categories, Post.PostSpec postSpec) {
        client.list(Category.class, null, null)
            .collectList()
            .subscribe(categoryList -> {
                String meta_name ="";
                List<String> mns = new java.util.ArrayList<>(List.of());
                for (String category : categories) {
                    //分类名是否已存在
                    boolean isExist = false;
                    for (Category t : categoryList) {
                        if (t.getSpec().getDisplayName().equals(category)) {
                            isExist = true;
                            //存在，获取元数据名
                            meta_name = t.getMetadata().getName();
                            break;
                        }
                    }
                    //不存在则创建
                    if (!isExist) {
                        Category newCategory = new Category();
                        Category.CategorySpec spec = new Category.CategorySpec();
                        Metadata metadata = new Metadata();
                        //设置分类的属性
                        spec.setDisplayName(category);
                        spec.setSlug(UUID.fastUUID().toString(false));
                        spec.setCover("");
                        spec.setChildren(new ArrayList<>());
                        spec.setDescription("");
                        spec.setTemplate("");
                        spec.setPriority(0);
                        //设置元数据
                        meta_name = UUID.fastUUID().toString(false);
                        metadata.setName(meta_name);
                        newCategory.setSpec(spec);
                        newCategory.setMetadata(metadata);
                        Category.CategoryStatus status = new Category.CategoryStatus();
                        status.setPostCount(0);
                        newCategory.setStatus(status);
                        //创建新的分类
                        Mono.defer(() -> client.create(newCategory)).subscribe();
                    }
                    mns.add(meta_name);
                }
                postSpec.setCategories(mns);
            });
    }
}