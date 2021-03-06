/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.NewRatingEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingsResource extends AbstractResource {
    
    @Autowired
    private RatingService ratingService;
    
    @Inject
    private RatingMapper ratingMapper;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiRatingsByApiId(@PathParam("apiId") String apiId, @BeanParam PaginationParam paginationParam) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a->a.getId().equals(apiId))) {
            final Page<RatingEntity> ratingEntityPage = ratingService.findByApi(apiId, 
                    new PageableBuilder()
                        .pageNumber(paginationParam.getPage())
                        .pageSize(paginationParam.getSize())
                        .build()
                    );
            
            List<Rating> ratings = ratingEntityPage.getContent().stream()
                    .map(ratingMapper::convert)
                    .collect(toList());

            //No pagination, because ratingService did it already
            return createListResponse(ratings, paginationParam, false);
        }
        throw new ApiNotFoundException(apiId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.CREATE)
    })
    public Response createApiRatingForApi(@PathParam("apiId") String apiId, @Valid RatingInput ratingInput) {
        if(ratingInput == null) {
            throw new BadRequestException("Input must not be null.");
        }
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a->a.getId().equals(apiId))) {
            NewRatingEntity rating = new NewRatingEntity();
            rating.setApi(apiId);
            rating.setComment(ratingInput.getComment());
            rating.setTitle(ratingInput.getTitle());
            rating.setRate(ratingInput.getValue().byteValue());
            
            RatingEntity createdRating = ratingService.create(rating);
            
            return Response
                    .status(Status.CREATED)
                    .entity(ratingMapper.convert(createdRating))
                    .build();
        }
        throw new ApiNotFoundException(apiId);
    }
}
