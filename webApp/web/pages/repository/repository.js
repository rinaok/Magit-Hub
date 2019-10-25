var REFRESH_WC = 0;
var GET_PAGE_DATA = 1;
var GET_COMMIT_FILES = 2;
var GET_FILE_CONTENT = 3;
var GET_OPEN_CHANGES = 4;

var EDIT_FILE = 4;
var DELETE_FILE = 5;
var NEW_FILE = 6;
var COMMIT = 7;

var PULL = 0;
var PUSH = 1;
var PR = 2;


$(function() { // onload...do
    $('#addFileModal').find('.modal-header h8').hide();
    $("#successAlert").hide();
    var data = "reqType="+ GET_PAGE_DATA;
    $.ajax({
        method: 'GET',
        url: "repo",
        dataType: 'json',
        data : data,
        timeout: 4000,
        error: function (e) {
            console.log(e.responseText);
        },
        success: function (r) {
            var branches = r.branches;
            var commits = r.commits;
            var wc = r.wcFiles;
            showBranchesData(branches);
            showCommits(commits);
            showWCFiles(wc);
            showOpenChanges();
            showCollaborationOptions(r.isForked);
            fillBranchesToPR(branches);
            showPullRequests(r.pullRequests);
        }
    })
});

function showPullRequests(pullRequests) {
    var branchesList = $('#branchesList');
    for (var i = 0 ; i < pullRequests.length; i++) {
        var targetBranch = pullRequests[i].targetBranch;
        var colorClass = "<li class='list-group-item list-group-item-light'>";
        branchesList.append(
            colorClass + "<span class = 'badge badge-primary'>Pull Request</span>" +
            "<div style='max-width: fit-content; max-height: 10px;'>" +
            "<h6><b>Target Branch </b>" + targetBranch + "</h6>" +
            "<p><b>Base Branch </b>" + pullRequests[i].baseBranch + "</p>" +
            "<p><b>Message </b>" + pullRequests[i].msg + "</p>" +
            "</div>" +
            "<div>" +
            "<input id=\"AcceptPR\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Accept PR\" style='float: right; margin-right: 10px;'>" +
            "<input id=\"Details\" type=\"submit\" class=\"btn btn-info pull-right\" value=\"Details\" style=\"float: right;\">" +
            "</div>" +
            "</div>");
    }
}

function showCollaborationOptions(isForked){
    if(isForked){
        document.getElementById("push").disabled = false;
        document.getElementById("pull").disabled = false;
        document.getElementById("pullRequest").disabled = false;
    }
    else{
        document.getElementById("collaborationArea").innerHTML = "Collaboration options are unavailable " +
            "for this repository since it is not forked";
        document.getElementById("push").disabled = true;
        document.getElementById("pull").disabled = true;
        document.getElementById("pullRequest").disabled = true;
    }
}

$(document).on('click', '#pull', function (event) {
    var data = "reqType=" + PULL;
    $.ajax({
        method: 'GET',
        data: data,
        url: "collaboration",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            alert(r);
            location.reload();
        }
    });
});

$(document).on('click', '#push', function (event) {
    var data = "reqType=" + PUSH;
    $.ajax({
        method: 'GET',
        data: data,
        url: "collaboration",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            alert(r);
            location.reload();
        }
    });
});

function showOpenChanges(){
    var data = "reqType=" + GET_OPEN_CHANGES;
    $.ajax({
        method: 'GET',
        data: data,
        url: "repo",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (changes) {
            $('#openChangesCard').find('.card-text').text(changes);
            showFiles(changes.newFiles, $('#newFilesList'));
            showFiles(changes.deletedFiles, $('#deletedFilesList'));
            showFiles(changes.modifiedFiles, $('#modifiedFilesList'));
        }
    });
}

function showFiles(files, list) {
    list.empty();
    for (var i = 0; i < files.length; i++) {
        list.append("<li class='list-group-item'>" + files[i] + "</li>");
    }
}

function showBranchesData(branches){
    var branchesList = $('#branchesList');
    branchesList.empty();
    for (var i = 0 ; i < branches.length; i++) {
        var branch = branches[i];
        var colorClass = "<li class='list-group-item list-group-item-light'>";
        var Head = "";
        if(branch.isHead) {
            colorClass = "<li class='list-group-item list-group-item-primary'>";
            Head = " || Head Branch";
        }
        branchesList.append(
            colorClass +
            "<div style='max-width: fit-content; max-height: 10px;'>" +
            "<h6>" + branch.name + Head +"</h6>" +
            "<p>" + branch.commitSha1 + "</p>" +
            "</div>" +
            "<div>" +
            "<input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-danger pull-right\" value=\"Delete\" style=\"float: right;\">" +
            "<input id=\"checkout\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Checkout\" style='float: right; margin-right: 10px;'>" +
            "</div>" +
            "</div>");
    }
}

function showWCFiles(files) {
    $('#files-tree').empty();
    $('#files-tree').treeview({
        data: files,
        showTags: true,
        levels: 20,
        onNodeSelected: function (event, data) {
            if (data.nodes.length > 0) { // folder
                $('#addFileModal').find('.modal-header h8').text(data.filePath);
                document.getElementById("addFileBtn").disabled = false;
            } else // file
            {
                document.getElementById("addFileBtn").disabled = true;
                var filePath = data.filePath;
                filePath = filePath.replace(/\\/g,"//");
                var request = "reqType=" + GET_FILE_CONTENT + "&filePath=" + filePath;
                $.ajax({
                    url: 'repo',
                    type: 'GET',
                    data: request,
                    error: function (e) {
                        alert(e.responseText);
                    },
                    success: function (response) {
                        // Add response in Modal body
                        document.getElementById("content").value = response;
                        // Display Modal
                        $('#fileContentModal').find('.modal-header h2').text(data.text);
                        $('#fileContentModal').find('.pl-4').text(data.filePath);
                        $('#fileContentModal').modal('show');
                    }
                });
            }
        }
    });
}

function showCommitFiles(files) {
    var table = document.getElementById("commitFilesTable");
    $("#commitFilesTable").find("tbody").empty();
    var tableBody = "";
    tableBody = tableBody + "<tr>";
    files.forEach(function(row){
            tableBody = tableBody + "<tr>";
            for (var j = 0; j < table.rows.length; j++){
                tableBody = tableBody + "<td><b>Name </b>" + row["name"] + "<br><b>Type </b>" +
                row["type"] + "<br><b>SHA1 </b>" + row["sha1"] + "<br><b>Modified By </b>" + row["lastModifier"] +
                    "<br><b>Modification Date </b><" + row["modificationDate"] + "</td>";
            }
            tableBody = tableBody + "</tr>";
        }
    );
    $('#commitFilesTable tbody').append(tableBody);
}


function showCommits(commits){
    var tableBody = "";
    tableBody = tableBody + "<tr>";
    var table = document.getElementById("commitsTable");

    // Create the data rows.
// Create the data rows.
    commits.forEach(function(row) {
        // Create a new row in the table for every element in the data array.
        tableBody = tableBody + "<tr style=\"cursor:pointer\">";
        var btn = "<input id=\"createBranch\" type=\"submit\" class=\"btn btn-primary\" value=\"Create Branch\" +/>";
        for (var j = 0; j < table.rows.length; j++){
            tableBody = tableBody + "<td>" + row["sha1"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["message"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["creationDate"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["createdBy"] + "</td>";
            tableBody = tableBody + "<td>" + row["pointingBranches"] + "</td>";
            tableBody = tableBody + "<td>" + btn + "</td>";
        }
        tableBody = tableBody + "</tr>";
    });

    $('#commitsTable tbody').append(tableBody);

    for (var i = 1; i < table.rows.length; i++) {
        for (var j = 0; j < table.rows[i].cells.length; j++) {
            table.rows[i].onclick = function (e) {
                var commitSha1 = e.currentTarget.firstChild.innerText;
                var data = "reqType=" + GET_COMMIT_FILES + "&commitSha1=" + commitSha1;
                $.ajax({
                    method: 'GET',
                    data: data,
                    url: "repo",
                    processData: false, // Don't process the files
                    contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                    timeout: 4000,
                    error: function (e) {
                        alert(e.responseText);
                    },
                    success: function (files) {
                        showCommitFiles(files);
                    }
                });
            }
        }
    }
}

$(document).on('click', '#createBranch', function (event) {
    event.preventDefault();
    var sha1 = event.currentTarget.parentElement.parentElement.cells[0].innerText;
    var branchName = prompt("Enter a Branch Name", "Branch Name");
    var data = sha1 + "&" + branchName;
    $.ajax({
        method: 'PUT',
        data: data,
        url: "repo",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            showBranchesData(r);
            alert("Branch created successfully");
        }
    });
});

$(document).on('click', '#deleteBranch', function (event) {
    event.preventDefault();
    var branchName = event.currentTarget.parentElement.parentElement.cells[0].innerText;
    debuggerS
    var data = branchName;
    $.ajax({
        method: 'DELETE',
        data: data,
        url: "repo",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            showBranchesData(r);
            alert("Branch deleted successfully");
        }
    });
});

$(document).on('click', '#editBtn', function (event) {
    event.preventDefault();
    var $modal = $('#fileContentModal');
    $("#content").attr("readonly", false);
    document.getElementById('saveBtn').style.visibility = 'visible';
    document.getElementById('cancelBtn').style.visibility = 'visible';
    document.getElementById("editBtn").disabled = true;
});

$(document).on('show.bs.modal', '#fileContentModal', function (e) {
    $("#content").attr("readonly", true);
    document.getElementById('saveBtn').style.visibility = 'hidden';
    document.getElementById('cancelBtn').style.visibility = 'hidden';
    document.getElementById("editBtn").disabled = false;
});

$(document).on('click', '#cancelBtn', function (event) {
    event.preventDefault();
    var $modal = $('#fileContentModal');
    $("#content").attr("readonly", false);
    document.getElementById('saveBtn').style.visibility = 'hidden';
    document.getElementById('cancelBtn').style.visibility = 'hidden';
    document.getElementById("editBtn").disabled = false;
});

$(document).on('click', '#saveBtn', function (event) {
    event.preventDefault();
    $("#content").attr("readonly", true);
    document.getElementById('saveBtn').style.visibility = 'hidden';
    document.getElementById('cancelBtn').style.visibility = 'hidden';
    document.getElementById("editBtn").disabled = false;
    var path = $('#fileContentModal').find('.pl-4')[0].innerHTML;
    path = path.replace(/\\/g,"//");
    var fileData = "reqType=" + EDIT_FILE + "&filePath=" + path + "&content=" + $("#content").val();
    $.ajax({
        url: 'wc',
        type: 'POST',
        data: fileData,
        error: function (e) {
            alert(e.response);
        },
        success: function (response) {
            $("#successAlert").show();
            setTimeout(function(){
                $("#successAlert").hide();
            }, 2000);
            $('#fileContentModal').modal('hide');
            showOpenChanges();
        }
    });
});

$(document).on('click', '#deleteBtn', function (event) {
    event.preventDefault();
    var $modal = $('#fileContentModal');
    var result = confirm("Are you sure you want to delete this file?");
    if(result){
        var path = $('#fileContentModal').find('.pl-4')[0].innerHTML;
        path = path.replace(/\\/g,"//");
        var fileData = "reqType=" + DELETE_FILE + "&filePath=" + path;
        $.ajax({
            url: 'wc',
            type: 'POST',
            data: fileData,
            error: function (e) {
                alert(e.response);
            },
            success: function (response) {
                $("#successAlert").show();
                setTimeout(function(){
                    $("#successAlert").hide();
                }, 2000);
                $('#fileContentModal').modal('hide');
                showOpenChanges();
            }
        });
    }
});

$(document).on('click', '#addFileBtn', function (event) {
    var filePath = $('#addFileModal').find('.modal-header h8').text();
    if(filePath.localeCompare("")) {
        event.preventDefault();
        $('#fileContent').val("");
        $('#fileName').val("");
        $('#addFileModal').modal('show');
    }
    else{
        alert("Please choose a folder to add the file into");
    }
});

function refreshWC(){
    var req = "reqType=" + REFRESH_WC;
    $.ajax({
        url: 'repo',
        type: 'GET',
        data: req,
        error: function (e) {
            alert(e.response);
        },
        success: function (response) {
            showWCFiles(response);
        }
    });
}

$(document).on('click', '#submitBtn', function (event) {
    var filePath = $('#addFileModal').find('.modal-header h8').text();
    filePath = filePath.replace(/\\/g,"//");
    var content = $("#fileContent").val();
    var fileName = $("#fileName").val();
    var fileData = "reqType=" + NEW_FILE + "&fileName=" + fileName + "&fileContent=" + content + "&filePath=" + filePath;
    $.ajax({
        url: 'wc',
        type: 'POST',
        data: fileData,
        error: function (e) {
            alert(e.response);
        },
        success: function (response) {
            $("#successAlert").show();
            setTimeout(function(){
                $("#successAlert").hide();
            }, 2000);
            $('#addFileModal').modal('hide');
            showOpenChanges();
        }
    });
});

$(document).on('click', '#commitBtn', function (event) {
    var msg = prompt("Please enter a commit message:");
    if (msg != null && msg != "") {
        var data = "reqType=" + COMMIT + "&msg=" + msg;
        $.ajax({
            url: 'wc',
            type: 'POST',
            data: data,
            error: function (e) {
                alert(e.response);
            },
            success: function (response) {
                $("#successAlert").show();
                setTimeout(function () {
                    $("#successAlert").hide();
                }, 2000);
                $('#addFileModal').modal('hide');
                showOpenChanges();
                refreshWC();
                location.reload();
            }
        });
    }
});

$(document).on('click', '#pullRequest', function (event) {
    $('#pullRequestModal').modal('show');
});


$(document).on('click', '#submitPRBtn', function (event) {
    var branches = document.getElementsByClassName("list-group-item active");
    var target;
    var base;
    if(branches.length == 2){
        for(var i = 0; i < 2; i++){
            if(branches[i].parentElement.id == "targetBranches"){
                target = branches[i].innerHTML;
            }
            else if (branches[i].parentElement.id == "baseBranches"){
                base = branches[i].innerHTML;
            }
        }
        if(base != null && target != null){
            var msg = document.getElementById("prMsg").innerHTML;
            var data = "reqType=" + PR + "&target=" + target.replace(/\\/g,"//") + "&base=" + base.replace(/\\/g,"//") + "&msg=" + msg;
            $.ajax({
                method: 'GET',
                data: data,
                url: "collaboration",
                processData: false, // Don't process the files
                contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                timeout: 4000,
                error: function (e) {
                    alert(e.responseText);
                },
                success: function (r) {
                    alert(r);
                    location.reload();
                }
            });
        }
    }
    else{
        alert("Please select base and target branches");
    }
});


function fillBranchesToPR(branches){
    var targetBranches = $('#targetBranches');
    targetBranches.empty();
    for (var i = 0; i < branches.length; i++) {
        if(branches[i].tracking && !branches[i].isRemote)
            targetBranches.append("<a href=\"#\" class='list-group-item list-group-item-action' data-toggle=\"list\"'>" + branches[i].name + "</a>");
    }

    var baseBranches = $('#baseBranches');
    for (var i = 0; i < branches.length; i++) {
        if(branches[i].tracking || branches[i].isRemote)
            baseBranches.append("<a href=\"#\" class='list-group-item list-group-item-action' data-toggle=\"list\"'>" + branches[i].name + "</a>");
    }
}

$(function(){
    $('.list-group li').click(function(e) {
        e.preventDefault()
        $that = $(this);
        $that.parent().find('a').removeClass('active');
        $that.addClass('active');
    });
})
