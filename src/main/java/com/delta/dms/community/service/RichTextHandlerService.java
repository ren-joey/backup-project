package com.delta.dms.community.service;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.delta.dms.community.enums.DdfDocCat;
import com.delta.dms.community.swagger.model.FileType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import com.delta.datahive.api.DDF.BaseSection;
import com.delta.datahive.api.DDF.DDF;
import com.delta.datahive.api.DDF.PrivilegeSection;
import com.delta.datahive.api.DDF.PrivilegeType;
import com.delta.datahive.api.DDF.ServiceType;
import com.delta.datahive.api.DDF.UserGroupEntity;
import com.delta.datahive.types.DDFStatus;
import com.delta.datahive.types.SystemTag;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.DdfType;
import com.delta.dms.community.swagger.model.RichTextImage;
import com.delta.dms.community.utils.Constants;

@Service
public class RichTextHandlerService {

  private FileService fileService;

  private static final String HTML_IMAGE_SRC = "img[src]";
  private static final String HTML_SRC = "src";
  private static final String DATAHIVE_STORE = "DH_STORE";
  private static final String DATAHIVE_URL_PREFIX = "/" + DATAHIVE_STORE + "/DMS_Image/";
  private static final String DATAHIVE_URL_WITH_UUID =
      DATAHIVE_URL_PREFIX + "\\S{8}-\\S{4}-\\S{4}-\\S{4}-\\S{12}";
  private static final String DATAHIVE_URL_REGEX_WITH_START_WITH = "^" + DATAHIVE_URL_WITH_UUID;
  private static final String BASE64_PREFIX_REGEX = "data:image\\/([a-zA-Z]*);base64,([^\\\"]*)";
  private static final String BASE64_MIMETYPE_REGEX = "^data:([a-zA-Z0-9]+/[a-zA-Z0-9]+).*,.*";
  private static final String EMBEDDEDIMAGE_DEFAULT_EXT = "png";
  private static final String PATH_CACHE_STORE = "cac/store";

  public RichTextHandlerService(FileService fileService) {
    this.fileService = fileService;
  }

  public RichTextImage replaceRichTextImageSrcWithImageDataHiveUrl(
      ForumType forumType,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      String text) {
    String html = "";
    if (!StringUtils.isEmpty(text)) {
      html = getHtmlWithDataHiveUrl(forumType, roleMap, privilegeMap, text);
    }
    return new RichTextImage().text(html).refIds(new ArrayList<>());
  }

  public String changeToCacheUrl(String text) {
    return ofNullable(text)
        .filter(StringUtils::isNotEmpty)
        .map(
            richText -> {
              Document html = Jsoup.parse(text);
              html.outputSettings().prettyPrint(false);
              for (Element el : html.select(HTML_IMAGE_SRC)) {
                String dataHiveUrl = getDataHiveUrl(el.attr(HTML_SRC));
                ofNullable(dataHiveUrl)
                    .filter(StringUtils::isNotEmpty)
                    .ifPresent(
                        url -> {
                          url = url.replace(DATAHIVE_STORE, PATH_CACHE_STORE);
                          el.attr(HTML_SRC, url);
                        });
              }
              return html.body().html();
            })
        .orElseGet(() -> EMPTY);
  }

  public String removeCacheParameter(String text) {
    return StringUtils.defaultString(text).replace(PATH_CACHE_STORE, DATAHIVE_STORE);
  }

  private DDF setEmbeddedImageDdf(
      ForumType forumType,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap) {
    DDF ddf =
        new DDF()
            .setBaseSection(
                new BaseSection()
                    .setName(UUID.randomUUID().toString())
                    .setStatus(DDFStatus.DRAFT)
                    .setInternalCategory(DdfDocCat.TOPIC_RICHTEXTIMAGE.toString())
                    .setIcon(FileType.IMG.toString())
                    .setServiceType(ServiceType.IMAGE))
            .setPrivilegeSection(new PrivilegeSection().setIdMap(privilegeMap));
    if (ForumType.PRIVATE.equals(forumType)) {
      ddf.getPrivilegeSection().removePublic(FileService.PRIV_ALL);
    } else {
      ddf.getPrivilegeSection().addPublic(FileService.PRIV_PUBLIC_SR);
      ddf.getBaseSection().setSystemTags(getNoWatermarkSystemTags());
    }
    roleMap
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));
    return ddf;
  }

  private String getHtmlWithDataHiveUrl(
      ForumType forumType,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      String text) {
    Document html = Jsoup.parse(text);
    html.outputSettings().prettyPrint(false);
    for (Element el : html.select(HTML_IMAGE_SRC)) {
      String imageSrc = el.attr(HTML_SRC);
      if (!isDataHiveUrl(imageSrc) && isBase64(imageSrc)) {
        DDF ddf = setEmbeddedImageDdf(forumType, roleMap, privilegeMap);
        String base64 = imageSrc.split(Constants.COMMA_DELIMITER)[1];
        String uuid =
            fileService.createFile(
                ddf,
                ddf.getBaseSection().getName() + FileService.SYM_EXT + extractFileExt(imageSrc),
                Base64.decodeBase64(base64.getBytes(StandardCharsets.UTF_8)));
        el.attr(HTML_SRC, DATAHIVE_URL_PREFIX + uuid);
      }
    }
    return html.body().html();
  }

  private String extractFileExt(final String base64) {
    Pattern mime = Pattern.compile(BASE64_MIMETYPE_REGEX);
    Matcher matcher = mime.matcher(base64);
    String fileExt = EMBEDDEDIMAGE_DEFAULT_EXT;
    if (matcher.find()) {
      MimeType mimeType = MimeType.valueOf(matcher.group(1).toLowerCase());
      fileExt = mimeType.getSubtype();
    }
    return fileExt;
  }

  private boolean isDataHiveUrl(String imageSrc) {
    Pattern pattern = Pattern.compile(DATAHIVE_URL_REGEX_WITH_START_WITH);
    Matcher matcher = pattern.matcher(imageSrc);
    return matcher.find();
  }

  private String getDataHiveUrl(String imageSrc) {
    Pattern pattern = Pattern.compile(DATAHIVE_URL_REGEX_WITH_START_WITH);
    Matcher matcher = pattern.matcher(imageSrc);
    if (matcher.find()) {
      return matcher.group();
    }
    return EMPTY;
  }

  private boolean isBase64(String imageSrc) {
    Pattern pattern = Pattern.compile(BASE64_PREFIX_REGEX);
    Matcher matcher = pattern.matcher(imageSrc);
    return matcher.find();
  }

  private Set<String> extractImageIdFromRichText(String html) {
    Set<String> imageIdList = new HashSet<>();
    Pattern pattern = Pattern.compile(DATAHIVE_URL_WITH_UUID);
    Document doc = Jsoup.parse(html);
    doc.outputSettings().prettyPrint(false);
    for (Element el : doc.select(HTML_IMAGE_SRC)) {
      String imageSrc = el.attr(HTML_SRC);
      Matcher matcher = pattern.matcher(imageSrc);
      while (matcher.find()) {
        imageIdList.add(matcher.group().replaceAll(DATAHIVE_URL_PREFIX, ""));
      }
    }
    return imageIdList;
  }

  public void deleteRemovedImageInRichText(String originalText, String newText, Integer associatedId) {
    Set<String> originalImageIdList = extractImageIdFromRichText(originalText);
    Set<String> newImageIdList = extractImageIdFromRichText(newText);
    originalImageIdList
        .stream()
        .filter(item -> !newImageIdList.contains(item))
        .forEach(item -> fileService.delete(item, DdfType.FILE.toString(), associatedId));
  }

  public String replaceCopyedImageToBase64(String newText, String originalText) {
    Set<String> originalImageIdList = extractImageIdFromRichText(originalText);
    Set<String> imageIdList = extractImageIdFromRichText(newText);
    imageIdList.removeAll(originalImageIdList);
    Map<String, String> imageToBase64Map = new HashMap<>();
    imageIdList.forEach(
        imageId -> {
          DDF ddf = fileService.readDdf(imageId, FileService.DDF_BASE_FIELD);
          if (ddf.getBaseSection() != null
              && ServiceType.IMAGE.equals(ddf.getBaseSection().getServiceType())) {
            byte[] image = fileService.readRawData(imageId);
            String base64 =
                new StringBuilder("data:image/")
                    .append(ddf.getBaseSection().getFileExt().toLowerCase())
                    .append(";base64,")
                    .append(Base64.encodeBase64String(image))
                    .toString();
            imageToBase64Map.put(imageId, base64);
          }
        });
    Document doc = Jsoup.parse(newText);
    doc.outputSettings().prettyPrint(false);
    for (Element el : doc.select(HTML_IMAGE_SRC)) {
      String imageSrc = el.attr(HTML_SRC);
      if (imageToBase64Map.keySet().stream().anyMatch(imageSrc::contains)) {
        newText =
            newText.replaceAll(
                imageSrc,
                imageToBase64Map.get(imageSrc.replaceAll(".*" + DATAHIVE_URL_PREFIX, "")));
      }
    }
    return newText;
  }

  private Map<SystemTag, String> getNoWatermarkSystemTags() {
    Map<SystemTag, String> systemTags = new EnumMap<>(SystemTag.class);
    systemTags.put(SystemTag.NO_WATERMARK, EMPTY);
    return systemTags;
  }
}
